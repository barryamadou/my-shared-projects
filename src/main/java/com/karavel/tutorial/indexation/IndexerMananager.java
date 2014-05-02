package com.karavel.tutorial.indexation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;

import com.karavel.front.send.request.model.ResponseOut;
import com.karavel.tutorial.rest.IndexerMananagerOut;
import com.karavel.tutorial.conf.MailSender;
import com.karavel.tutorial.conf.SmtpHeaderWelcomeNewMemberStrategy;
import com.karavel.tutorial.indexation.db.DbSharder;
import com.karavel.tutorial.indexation.elasticsearch.EsAdminManager;
import com.karavel.tutorial.indexation.elasticsearch.IndexationResult;

public class IndexerMananager {

	private Logger LOGGER = LogManager.getLogger(IndexerMananager.class);

	public Boolean isIndexationFullInProgress = Boolean.FALSE;
	public Boolean isIndexationDeltaInProgress = Boolean.FALSE;
	public final static String BATCHTYPE_FULL="FULL";
	public final static  String BATCHTYPE_DELTA="DELTA";
	public final static SimpleDateFormat simpleDateFormatForLog=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	private EsAdminManager esAdminManager;
	private DbSharder dbSharder;
	private Properties configProperties;
	private MailSender mailSender;

	

	public DbSharder getDbSharder() {
		return dbSharder;
	}

	public void setDbSharder(DbSharder dbSharder) {
		this.dbSharder = dbSharder;
	}

	private HashMap<String, Object> getAndOverWriteConfig(MultivaluedMap<String, String> inputParameter){
		HashMap<String, Object> usedParams=new HashMap<String, Object>();
		Set<Object> listKeys = configProperties.keySet();
		for (Object key : listKeys) {
			if(inputParameter!=null && inputParameter.containsKey(key.toString())){
				usedParams.put(key.toString(), inputParameter.getFirst(key.toString()));
			}else{
				usedParams.put(key.toString(), configProperties.getProperty(key.toString()));
			}
		}
		return usedParams;
	}
	
	public IndexerMananagerOut startIndexationFull(MultivaluedMap<String, String> inputParameter) throws Exception {
		IndexerMananagerOut indexerMananagerOut=new IndexerMananagerOut();
		int nbrTotaIndexed=0;
		boolean isOk = checkIsIndexationFullPossible();
		if (!isOk) {
			indexerMananagerOut.setMessage("Indexation is already in progress");
			indexerMananagerOut.setResultatCode(-1);
			return indexerMananagerOut;
		}
		//initiate
		long startIndexation = System.currentTimeMillis();
		Hashtable<String, Object> meticsInfos=new Hashtable<String, Object>();
		SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd");
		Date startDbFoSnapshotTimestamp=simpleDateFormat.parse("1975/01/01");
		Date endDbFoSnapshotTimestamp=new Date(startIndexation);
		HashMap<String, Object> configPropertiesUsed=getAndOverWriteConfig(inputParameter);
		String aliasName=(String)configPropertiesUsed.get("index.alias.name");
		boolean isDeltaBatch=false;
		String msg=isDeltaBatch?"DELTA":"FULL";
		
		try {
			
			//send email
			msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " DEBUT";
			configPropertiesUsed.put("mail.subject", msg);
			configPropertiesUsed.put("startDbFoSnapshotTimestamp", startDbFoSnapshotTimestamp);
			configPropertiesUsed.put("endDbFoSnapshotTimestamp", endDbFoSnapshotTimestamp);
			mailSender.sendEmail(configPropertiesUsed);
			
			// check does alias and index exist
			String indexNameTheAliasPointsTo=esAdminManager.doesAliasExist(aliasName);
			String user=configPropertiesUsed.get("connected.user").toString();
			if(StringUtils.hasText(indexNameTheAliasPointsTo)){

				//get the complementary name
				String newIndexName = esAdminManager.rotationGetNewIndexName(indexNameTheAliasPointsTo);

				// create index mapping
				boolean deleteAndCreate=true;
				esAdminManager.createIndexAndMapping(deleteAndCreate, newIndexName,  isDeltaBatch, configPropertiesUsed);
				
				// index docs using a channel
				ResponseOut<IndexationResult>[] logementShards = dbSharder.retrieveLogementShard(startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, newIndexName, aliasName, isDeltaBatch, configPropertiesUsed);
				
				// perform alias name rotation 
				boolean isRoationDone = esAdminManager.rotateAliasNames(aliasName,newIndexName, indexNameTheAliasPointsTo) ;
				
				// close old Index
				boolean isIndexClosed = esAdminManager.closeIndex(indexNameTheAliasPointsTo) ;
				
				// create replica
				esAdminManager.updateNumberOfReplica(Integer.parseInt(configPropertiesUsed.get("number.of.replicas").toString()) , configPropertiesUsed.get("index.name").toString());

				// keep Metrics
				long endIndexation = System.currentTimeMillis();
				
				// end process indexation
				nbrTotaIndexed=0;
				for (int i = 0; i < logementShards.length; i++) {
					nbrTotaIndexed=nbrTotaIndexed+logementShards[i].get(DbSharder.CAMEL_FUTUR_TIMEOUT_SECOND).getInsertedDateNbrWithoutFailure();
				}
				// retrieve indexation stats
				long indexationTimeSecond = (endIndexation - startIndexation)/1000;
				long throuputDocsPerSecond = nbrTotaIndexed/indexationTimeSecond;
				meticsInfos.put("totalIndexedDocs", nbrTotaIndexed);
				meticsInfos.put("indexationTimeSecond", indexationTimeSecond);
				meticsInfos.put("throuputDocsPerSecond", throuputDocsPerSecond);
//				meticsInfos=esAdminManager.indexStats(newIndexName);
				
				
				extractInsertMetrics(user, startIndexation, endIndexation, meticsInfos,
						startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, BATCHTYPE_FULL, configPropertiesUsed );
				
				
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("*** All indexation processes finishes IN "
							+ indexationTimeSecond + " second ");
					LOGGER.info("*** throuputDocsPerSecond "
							+ throuputDocsPerSecond);
					LOGGER.info("*** startDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(startDbFoSnapshotTimestamp));
					LOGGER.info("*** endDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(endDbFoSnapshotTimestamp));
					LOGGER.info("*** batchType "
							+ BATCHTYPE_FULL);
					LOGGER.info("*** user "
							+ user);
					LOGGER.info("*** nbrTotaIndexed docs : "
							+ nbrTotaIndexed);
				}
				
			}else{
				// index and alias do not exist
				
				// create index mapping
				boolean deleteAndCreate=true;
				String theNewIndexName=EsAdminManager.INDEX_NAME_FOR_ROTATION + EsAdminManager.INDEX_NAME_DELIMETER + "0";
				esAdminManager.createIndexAndMapping(deleteAndCreate, theNewIndexName,  isDeltaBatch, configPropertiesUsed);
				
				// index docs using a channel
				ResponseOut<IndexationResult>[] logementShards = dbSharder.retrieveLogementShard(startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, theNewIndexName, aliasName, isDeltaBatch, configPropertiesUsed);
				
				// add alias to new index
				esAdminManager.addAliasToIndex(theNewIndexName, aliasName);
				
				// create replica
				esAdminManager.updateNumberOfReplica(Integer.parseInt(configPropertiesUsed.get("number.of.replicas").toString()) , configPropertiesUsed.get("index.name").toString());

				// keep Metrics
				long endIndexation = System.currentTimeMillis();

				// end process indexation
				for (int i = 0; i < logementShards.length; i++) {
					nbrTotaIndexed=nbrTotaIndexed+logementShards[i].get(DbSharder.CAMEL_FUTUR_TIMEOUT_SECOND).getInsertedDateNbrWithoutFailure();
				}
				
				// retrieve indexation stats
				long indexationTimeSecond = (endIndexation - startIndexation)/1000;
				long throuputDocsPerSecond = nbrTotaIndexed/indexationTimeSecond;
				meticsInfos.put("totalIndexedDocs", nbrTotaIndexed);
				meticsInfos.put("indexationTimeSecond", indexationTimeSecond);
				meticsInfos.put("throuputDocsPerSecond", throuputDocsPerSecond);
//				meticsInfos=esAdminManager.indexStats(theNewIndexName);
				
				extractInsertMetrics(user, startIndexation, endIndexation, meticsInfos,
						startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, BATCHTYPE_FULL, configPropertiesUsed);
				
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("*** All indexation processes finishes IN "
							+ indexationTimeSecond + " second ");
					LOGGER.info("*** throuputDocsPerSecond "
							+ throuputDocsPerSecond);
					LOGGER.info("*** startDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(startDbFoSnapshotTimestamp));
					LOGGER.info("*** endDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(endDbFoSnapshotTimestamp));
					LOGGER.info("*** batchType "
							+ BATCHTYPE_FULL);
					LOGGER.info("*** user "
							+ user);
					LOGGER.info("*** nbrTotaIndexed docs : "
							+ nbrTotaIndexed);
				}
				
			}
			
			//indeaxtion done
			indexerMananagerOut.setMessage("OK");
			indexerMananagerOut.setResultatCode(nbrTotaIndexed);
			
			//sending email
			msg=isDeltaBatch?"DELTA":"FULL";
			msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " FIN";
			configPropertiesUsed.put("mail.subject", msg);
			configPropertiesUsed.put("nbrTotaIndexed", nbrTotaIndexed);
			mailSender.sendEmail(configPropertiesUsed);		
			
		}
		
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			indexerMananagerOut.setMessage(e.getMessage());
			indexerMananagerOut.setResultatCode(-1);
			//sending email
			msg=isDeltaBatch?"DELTA":"FULL";
			msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " FAILED";
			configPropertiesUsed.put("mail.subject", msg);
			configPropertiesUsed.put("nbrTotaIndexed", nbrTotaIndexed);
			configPropertiesUsed.put("errorMessage", e.getMessage());
			mailSender.sendEmail(configPropertiesUsed);		
		} 
		finally {
			markIndexationFullPossible();
		}
		return indexerMananagerOut;

	}

	
	public IndexerMananagerOut startIndexationDelta(MultivaluedMap<String, String> inputParameter) throws Exception {
		IndexerMananagerOut indexerMananagerOut=new IndexerMananagerOut();
		int nbrTotaIndexed=0;

		boolean isOk = checkIsIndexationDeltaPossible();
		if (!isOk) {
			indexerMananagerOut.setMessage("Indexation delta is already in progress");
			indexerMananagerOut.setResultatCode(-1);
			return indexerMananagerOut;
		}
		//initiate
		long startIndexation = System.currentTimeMillis();
		Hashtable<String, Object> meticsInfos=new Hashtable<String, Object>();
		HashMap<String, Object> configPropertiesUsed=getAndOverWriteConfig(inputParameter);
		Date startDbFoSnapshotTimestamp=esAdminManager.retriveLastLaunch();
		Date endDbFoSnapshotTimestamp=new Date(startIndexation);
		String aliasName=(String)configPropertiesUsed.get("index.alias.name");
		boolean isDeltaBatch=true;
		String user=configPropertiesUsed.get("connected.user").toString();
		//send email
		String msg=isDeltaBatch?"DELTA":"FULL";
		msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " DEBUT";
		configPropertiesUsed.put("mail.subject", msg);
		configPropertiesUsed.put("startDbFoSnapshotTimestamp", startDbFoSnapshotTimestamp);
		configPropertiesUsed.put("endDbFoSnapshotTimestamp", endDbFoSnapshotTimestamp);

		try {

			
			
			mailSender.sendEmail(configPropertiesUsed);
			// check does alias and index exist
			String indexNameTheAliasPointsTo=esAdminManager.doesAliasExist(aliasName);
			if(StringUtils.hasText(indexNameTheAliasPointsTo)){

				
				// index docs using a channel
				ResponseOut<IndexationResult>[] logementShards = dbSharder.retrieveLogementShard(startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, indexNameTheAliasPointsTo, aliasName, isDeltaBatch, configPropertiesUsed);
				long endIndexation = System.currentTimeMillis();
				

				// end process indexation
				 nbrTotaIndexed=0;
				for (int i = 0; i < logementShards.length; i++) {
					nbrTotaIndexed=nbrTotaIndexed+logementShards[i].get(DbSharder.CAMEL_FUTUR_TIMEOUT_SECOND).getInsertedDateNbrWithoutFailure();
				}

				// retrieve indexation stats
//				meticsInfos=esAdminManager.indexStats(newIndexName);
				long indexationTimeSecond = (endIndexation - startIndexation)/1000;
				long throuputDocsPerSecond = nbrTotaIndexed/indexationTimeSecond;
				meticsInfos.put("totalIndexedDocs", nbrTotaIndexed);
				meticsInfos.put("indexationTimeSecond", indexationTimeSecond);
				meticsInfos.put("throuputDocsPerSecond", throuputDocsPerSecond);
				
				
				// keep Metrics
				extractInsertMetrics(user, startIndexation, endIndexation, meticsInfos,
						startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, BATCHTYPE_DELTA, configPropertiesUsed);
				
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("*** All indexation processes finishes IN "
							+ indexationTimeSecond + " second ");
					LOGGER.info("*** throuputDocsPerSecond "
							+ throuputDocsPerSecond);
					LOGGER.info("*** startDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(startDbFoSnapshotTimestamp));
					LOGGER.info("*** endDbFoSnapshotTimestamp "
							+ simpleDateFormatForLog.format(endDbFoSnapshotTimestamp));
					LOGGER.info("*** batchType "
							+ BATCHTYPE_FULL);
					LOGGER.info("*** user "
							+ user);
					LOGGER.info("*** nbrTotaIndexed docs : "
							+ nbrTotaIndexed);
				}
				
			}else{
				// end process indexation
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("*** Index does not have alias. No indexation delta is performed ");
				}
				throw new Exception("Index does not have alias. No indexation delta is performed");
			}
			indexerMananagerOut.setMessage("OK");
			indexerMananagerOut.setResultatCode(nbrTotaIndexed);
			//sending email
			msg=isDeltaBatch?"DELTA":"FULL";
			msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " FIN";
			configPropertiesUsed.put("mail.subject", msg);
			configPropertiesUsed.put("nbrTotaIndexed", nbrTotaIndexed);
			mailSender.sendEmail(configPropertiesUsed);		
			
		
		}
		
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			indexerMananagerOut.setMessage(e.getMessage());
			indexerMananagerOut.setResultatCode(-1);
			//sending email
			msg=isDeltaBatch?"DELTA":"FULL";
			msg=(String)configPropertiesUsed.get("mail.subject.prefixe") + msg + " FAILED";
			configPropertiesUsed.put("mail.subject", msg);
			configPropertiesUsed.put("nbrTotaIndexed", nbrTotaIndexed);
			configPropertiesUsed.put("errorMessage", e.getMessage());
			mailSender.sendEmail(configPropertiesUsed);		
			
		} finally {
			markIndexationDeltaPossible();
		}
		return indexerMananagerOut;

	}
	
	private void extractInsertMetrics(String user, long startIndexation, long endIndexation,
			Hashtable<String, Object> meticsInfos,
			Date startDbFoSnapshotTimestamp, Date endDbFoSnapshotTimestamp, String batchType, HashMap<String, Object> configPropertiesUsed)
			throws Exception {
		//insert metrics
		meticsInfos.put("numberOfReplicas",
				configPropertiesUsed.get("number.of.replicas"));
		meticsInfos.put("shardNumber",
				configPropertiesUsed.get("shard.number"));
		meticsInfos.put("indexationPacketSize",
				configPropertiesUsed.get("indexation.packet.size"));
		meticsInfos.put("concurrentConsumersNumber",
				configPropertiesUsed.get("concurrentConsumers.number"));
		meticsInfos.put("shardSliceNumber",
				configPropertiesUsed.get("shard.slice.number"));
		meticsInfos.put("elasticSearchNodesNames",
				configPropertiesUsed.get("elasticSearchNodes.names"));
		meticsInfos.put("esClusterName",
				configPropertiesUsed.get("es.clusterName"));

		
		// insert stats
		meticsInfos.put("indexStartDate", new Date(startIndexation));
		meticsInfos.put("indexEndDate", new Date(endIndexation));
		
		meticsInfos.put("startDbFoSnapshotTimestamp", startDbFoSnapshotTimestamp);
		meticsInfos.put("endDbFoSnapshotTimestamp", endDbFoSnapshotTimestamp);
		meticsInfos.put("user", user);
		meticsInfos.put("batchType", batchType);
		Set<String> keys = meticsInfos.keySet();
		for (String key : keys) {
			// keep metrics info inthe context
			configPropertiesUsed.put(key, meticsInfos.get(key));
		}
		
		
		esAdminManager.insertMetrics(meticsInfos);
	}
	

	synchronized private boolean checkIsIndexationFullPossible() {
		if (isIndexationFullInProgress) {
			LOGGER.error("Indexation full is already in progress");
			return false;
		} else {
			isIndexationFullInProgress = Boolean.TRUE;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(" acquiring lock for full indexation");
			}
			return true;
		}
	}
	synchronized private boolean checkIsIndexationDeltaPossible() {
		if (isIndexationDeltaInProgress || isIndexationFullInProgress) {
			String msg=isIndexationFullInProgress?BATCHTYPE_FULL:BATCHTYPE_DELTA;
			LOGGER.error("Indexation: '"+ msg +"'  is already in progress");
			return false;
		} else {
			isIndexationDeltaInProgress = Boolean.TRUE;
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(" acquiring lock for full indexation delta");
			}
			return true;
		}
	}

	synchronized private void markIndexationFullPossible() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(" releasing  lock for full indexation");
		}
		isIndexationFullInProgress = Boolean.FALSE;
	}
	
	synchronized private void markIndexationDeltaPossible() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(" releasing  lock for delata indexation");
		}
		isIndexationDeltaInProgress = Boolean.FALSE;
	}

	public Properties getConfigProperties() {
		return configProperties;
	}

	public void setConfigProperties(Properties configProperties) {
		this.configProperties = configProperties;
	}

	public MailSender getMailSender() {
		return mailSender;
	}

	public void setMailSender(MailSender mailSender) {
		this.mailSender = mailSender;
	}

	public EsAdminManager getEsAdminManager() {
		return esAdminManager;
	}

	public void setEsAdminManager(EsAdminManager esAdminManager) {
		this.esAdminManager = esAdminManager;
	}

}
