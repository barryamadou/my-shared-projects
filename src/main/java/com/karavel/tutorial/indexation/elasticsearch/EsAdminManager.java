package com.karavel.tutorial.indexation.elasticsearch;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tools.ant.taskdefs.condition.HasMethod;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.action.admin.indices.alias.exists.AliasesExistResponse;
import org.elasticsearch.action.admin.indices.alias.get.IndicesGetAliasesResponse;
import org.elasticsearch.action.admin.indices.close.CloseIndexResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.flush.FlushResponse;
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.stats.IndicesStatsResponse;
import org.elasticsearch.action.admin.indices.status.IndicesStatusResponse;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.metadata.AliasAction;
import org.elasticsearch.cluster.metadata.AliasAction.Type;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.indexing.IndexingStats;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.StringUtils;

import com.karavel.tutorial.conf.SplunkKpiWritter;
import com.karavel.tutorial.indexation.IindexationService;
import com.karavel.tutorial.indexation.db.LogementShard;

public class EsAdminManager implements IindexationService {

	public static Integer ELASTCSEAR_SERVER_CONNECTION_TIMEOUT = 300;
	private Logger LOGGER = LogManager.getLogger(EsAdminManager.class);
	private Logger LOGGER_KPI = LogManager.getLogger("com.karavel.tutorial.indexation.EsManager" + ".KPI"); 
	public static String INDEX_NAME_DELIMETER = "_";
	public static String INDEX_NAME_FOR_ROTATION = "idx1";

	public Hashtable<String, Object> indexStats(String indexName) throws Exception {
		IndicesStatsResponse res = transportClient
				.admin()
				.indices()
				.prepareStats(
						indexName)
				.all()
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);
		IndexingStats indexing = res.getTotal().getIndexing();
		long indexCount = indexing.getTotal().getIndexCount();
		TimeValue indexTime = indexing.getTotal().getIndexTime();
		double debitSecond = indexCount
				/ (indexTime.getSeconds() == 0 ? 0.0001 : indexTime
						.getSeconds());
		if(LOGGER.isInfoEnabled()){
			LOGGER.info("====================================== total indexed docs: " + indexCount);
		}
		if(LOGGER.isInfoEnabled()){
			LOGGER.info("====================================== indexation time  in seconde: "
					+ indexTime.getSeconds());
		}
		if(LOGGER.isInfoEnabled()){
			LOGGER.info("====================================== indexation time  in minute: "
					+ indexTime.getMinutes());
		}
		if(LOGGER.isInfoEnabled()){
			LOGGER.info("====================================== debit docs/s  "
					+ debitSecond);
		}

		Hashtable<String, Object> meticsInfos = new Hashtable<String, Object>();
		meticsInfos.put("totalIndexedDocs", indexCount);
		meticsInfos.put("indexationTimeSecond", indexTime.getSeconds());
		meticsInfos.put("throuputDocsPerSecond", debitSecond);
		
		
		return meticsInfos;
	}

	public FlushResponse indexFlush(String indexName) throws Exception {
		FlushResponse res = transportClient
				.admin()
				.indices()
				.prepareFlush(
						indexName)
				.setFull(true)
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);

		return res;
	}

	public UpdateSettingsResponse updateNumberOfReplica(int nbrReplica, String indexName ) throws Exception{
		if(doesIndexExists(indexName)){
			HashMap<String, Object> settings=new HashMap<String, Object>();
			settings.put("index.number_of_replicas", Integer.toString(nbrReplica));	
			return updateIndexeSetting(settings, indexName);
		}else{
			return null;
		}
	}

	
	public UpdateSettingsResponse updateIndexeSetting(
			HashMap<String, Object> settings, String indexName) throws Exception {
		
		 UpdateSettingsResponse res = transportClient.admin().indices()
				.prepareUpdateSettings(indexName)
				.setSettings(settings)
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);
		 
			if(LOGGER.isInfoEnabled()){
				Set<String> keys = settings.keySet();
				for (String key : keys) {
					LOGGER.info(key + ": has been updated with: '" + settings.get(key) + "' ");
				}
			}


		return res;
	}

	public RefreshResponse indexRefresh(String indexName) throws Exception {
		RefreshResponse res = transportClient
				.admin()
				.indices()
				.prepareRefresh(
						indexName)
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);

		return res;
	}

	public void persisterDistribution(final List<LogementShard> logementShards) {
		this.esVirageMetricsDataSourceSimpleJdbcTemplate
				.execute("delete from distribution");
		insertDistribution(logementShards);
		return;
	}

	public boolean closeIndex(String indexName) throws Exception {
		boolean exists = doesIndexExists(indexName);
		if (exists) {
			CloseIndexResponse res = transportClient
					.admin()
					.indices()
					.prepareClose(indexName)
					.execute()
					.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
							TimeUnit.SECONDS);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("indexName: '" + indexName + "' is closed");
			}
			return res.isAcknowledged();
		}
		return true;
	}
	
	public boolean addAliasToIndex(String indexName, String aliasName) throws Exception {
		boolean exists = doesIndexExists(indexName);
		if (exists) {
			 IndicesAliasesResponse res = transportClient
					.admin()
					.indices()
					.prepareAliases()
					.addAlias(indexName, aliasName)
					.execute()
					.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
							TimeUnit.SECONDS);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("alias: '" + aliasName + "' is added to indexName: '" + indexName + "'");
			}
			return res.isAcknowledged();
		}
		return true;
	}
	

	public boolean doesIndexExists(String indexName) throws Exception {
		IndicesExistsResponse res = transportClient
				.admin()
				.indices()
				.prepareExists(indexName)
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);
		boolean exists = res.isExists();
		if (LOGGER.isInfoEnabled()) {
			String msg = exists ? "exists" : "does not exist";
			LOGGER.info("indexName: '" + indexName + "' " + msg);
		}

		return exists;
	}

	private void insertDistribution(final List<LogementShard> logementShards) {
		String sqlQueryCreateLogementShard = " insert into distribution (rank, codeIata, logementId, nbr, shard) "
				+ " values (:rank, :codeIata, :logementId, :nbr, :shard) ";

		Map[] params = new Map[logementShards.size()];
		for (int i = 0; i < params.length; i++) {
			Map<String, Object> myMap = new HashMap<String, Object>();
			myMap.put("rank", logementShards.get(i).getRank());
			myMap.put("codeIata", logementShards.get(i).getCodeIata());
			myMap.put("logementId", logementShards.get(i).getLongementId());
			myMap.put("nbr", logementShards.get(i).getLogementShardSize());
			myMap.put("shard", logementShards.get(i).getShard());
			params[i] = myMap;
		}
		SqlParameterSource[] batch = SqlParameterSourceUtils
				.createBatch(params);
		String sql;
		BatchPreparedStatementSetter pss;

		int[] updateCounts = esVirageMetricsDataSourceNamedParameterJdbcTemplate
				.batchUpdate(sqlQueryCreateLogementShard, batch);
	}

	public String rotationGetNewIndexName(String currentIndexName)
			throws Exception {
		String newIndexName = null;
		if (StringUtils.hasText(currentIndexName)) {
			String[] res = StringUtils.split(currentIndexName,
					INDEX_NAME_DELIMETER);
			if (res != null && res.length == 2) {
				if ("0".equals(res[1]) && INDEX_NAME_FOR_ROTATION.equals(res[0])) {
					newIndexName = INDEX_NAME_FOR_ROTATION
							+ INDEX_NAME_DELIMETER + "1";
				} else if ("1".equals(res[1]) && INDEX_NAME_FOR_ROTATION.equals(res[0])) {
					newIndexName = INDEX_NAME_FOR_ROTATION
							+ INDEX_NAME_DELIMETER + "0";
				} else {
					String message = "currentIndexName: " + currentIndexName
							+ " is not compliant with the naming rule ";
					LOGGER.error(message);
					throw new Exception(message);
				}
			} else {
				String message = "currentIndexName: " + currentIndexName
						+ " is not compliant with the naming rule ";
				LOGGER.error(message);
				throw new Exception(message);
			}
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("currentIndexName: " + currentIndexName + " newIndexName " + newIndexName);
		}
		return newIndexName;
	}

	public String doesAliasExist(String aliasName) throws Exception {

		String existingIndexName = null;

		// Bascule l'alias de l'ancien index vers le nouvel index
		AliasesExistResponse aliasesExist = transportClient
				.admin()
				.indices()
				.prepareAliasesExist()
				.addAliases(aliasName)
				.addIndices("*")
				.execute()
				.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
						TimeUnit.SECONDS);
		boolean isExists = aliasesExist.isExists();
		if (isExists) {
			IndicesGetAliasesResponse virageAliases = transportClient
					.admin()
					.indices()
					.prepareGetAliases(aliasName)
					.addAliases(aliasName)
					.addIndices("*")
					.execute()
					.actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT,
							TimeUnit.SECONDS);
			Map<String, List<AliasMetaData>> aliases = virageAliases
					.getAliases();
			if (virageAliases != null && virageAliases.getAliases() != null
					&& virageAliases.getAliases().size() == 1) {
				Set<String> keySet = virageAliases.getAliases().keySet();
				for (String indexNameFound : keySet) {
					existingIndexName = indexNameFound;
					List<AliasMetaData> aliasVirage = virageAliases
							.getAliases().get(indexNameFound);
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info("the alias name: '" + aliasName
								+ "' points to the index name :'"
								+ indexNameFound + "' ");
					}
				}
			} else {
				String message = "More than one alias with the name: '"
						+ aliasName + "'" + "exists";
				LOGGER.error(message);
				throw new Exception(message);
			}
		} else {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("aliasName: '" + aliasName + "' does not exist ");
			}

		}
		return existingIndexName;
	}

	public boolean rotateAliasNames(String aliasName,
			String indexNameToRedirectTo, String indexNameToRemoveAliasFrom)
			throws Exception {

		IndicesAliasesRequestBuilder aliasesReqBuilder = transportClient
				.admin().indices().prepareAliases();
		aliasesReqBuilder.addAliasAction(new AliasAction(Type.ADD,
				indexNameToRedirectTo, aliasName));
		aliasesReqBuilder.addAliasAction(new AliasAction(Type.REMOVE,
				indexNameToRemoveAliasFrom, aliasName));
		IndicesAliasesResponse indicesAliasesResponse = null;
		try {
			indicesAliasesResponse = aliasesReqBuilder.execute().actionGet(
					ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
		} catch (Exception e) {
			String message1 = "Removing alias name: '" + aliasName + "' from "
					+ indexNameToRemoveAliasFrom
					+ " and pointing it toward index name: '"
					+ indexNameToRedirectTo + "'" + " FAILED ";
			LOGGER.error(message1, e);
			throw new Exception(message1);
		}

		if (indicesAliasesResponse != null
				&& indicesAliasesResponse.isAcknowledged()) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Removing alias name: '" + aliasName + "' from "
						+ indexNameToRemoveAliasFrom
						+ " and pointing it toward index name: '"
						+ indexNameToRedirectTo + "'" + " SUCCEEDED ");
			}
			return true;
		} else {
			String message1 = "Removing alias name: '" + aliasName + "' from "
					+ indexNameToRemoveAliasFrom
					+ " and pointing it toward index name: '"
					+ indexNameToRedirectTo + "'" + " FAILED ";
			LOGGER.error(message1);
			throw new Exception(message1);
		}
	}

	
	public void insertMetrics(Hashtable<String, Object> meticsInfos)
			throws Exception {
//		Number newId = insertMetric.executeAndReturnKey(meticsInfos);
		SplunkKpiWritter splunkKpiWritter=new SplunkKpiWritter("indexation_elasticsearch");
		SplunkKpiWritter res = splunkKpiWritter.datas(meticsInfos);
		if(LOGGER_KPI.isInfoEnabled()){
			LOGGER_KPI.info(res.toString());
		}
		Number newId = insertMetric.execute(meticsInfos);
	}

	public Date retriveLastLaunch()
	throws Exception {
		
		String sql="SELECT max(endDbFoSnapshotTimestamp) FROM metrics";
		SqlParameterSource sqlParameterSource=new MapSqlParameterSource();
		Timestamp lastLaunchDate=esVirageMetricsDataSourceNamedParameterJdbcTemplate.queryForObject(sql, sqlParameterSource, Timestamp.class);
		SimpleDateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		if(lastLaunchDate!=null){
			Date lastLaunchDateUtil = new Date (lastLaunchDate.getTime());
			String lastLaunchDateString=dateFormat.format(lastLaunchDateUtil);
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("====================================== the Last endDbFoSnapshotTimestamp (lastLauchdate) : "+  lastLaunchDateString);
			}
			return lastLaunchDateUtil;
		}else{
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("====================================== the Last endDbFoSnapshotTimestamp (lastLauchdate) : is Null. No indexation full done yet");
			}
			throw new Exception("the Last endDbFoSnapshotTimestamp (lastLauchdate) is Null. No indexation full has been done yet");
		}
}
	
	
	
	public void createIndexAndMapping(boolean deleteAndCreate, String indexName, boolean isDeltaBatch,HashMap<String, Object> configProperties) throws Exception {
		IndicesAdminClient indicesAdminClient = transportClient.admin()
				.indices();
		IndicesExistsResponse indicesExistsResponse = indicesAdminClient
				.prepareExists(
						indexName)
				.execute().actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
		boolean isExists = indicesExistsResponse.isExists();
		boolean acknowledged = false;

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("*********************************** jsonIndexeMapping: "
					+ jsonIndexeMapping);
		}
		if (!isExists) {
			CreateIndexResponse response = indicesAdminClient
					.prepareCreate(
							indexName)
					.setSettings(
							ImmutableSettings
									.settingsBuilder()
									.put("number_of_shards", configProperties.get("shard.number") )
									.put("number_of_replicas", 0 ))
					.addMapping(
							configProperties.get("index.type").toString(),
							jsonIndexeMapping)
					.execute()
					.get(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
			;
			acknowledged = response.isAcknowledged();
		} else if (deleteAndCreate) {

			if(LOGGER.isDebugEnabled()){
				LOGGER.debug("*********************************** indexe already exists deleteAndCreate");
			}
			DeleteIndexResponse res = indicesAdminClient
					.prepareDelete(
							indexName)
					.execute().actionGet(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
			res.isAcknowledged();
			CreateIndexResponse response = indicesAdminClient
					.prepareCreate(
							indexName)
					.setSettings(
							ImmutableSettings
									.settingsBuilder()
									.put("number_of_shards", configProperties.get("shard.number") )
									.put("number_of_replicas", 0 ))
					.addMapping(
							configProperties.get("index.type").toString(),
							jsonIndexeMapping)
					.execute()
					.get(ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
			;
			acknowledged = response.isAcknowledged();
			if(LOGGER.isDebugEnabled()){
				LOGGER.debug(acknowledged ? "********************* indexe created"
						: "**************** Indexe Not created");
			}
		} else {
			throw new Exception("indexe already exists");
		}
	}

	public IndexationResult indexDocs(LogementShard logementShard
		
	) {

		IndexationResult indexationResult = new IndexationResult();
		
		indexationResult.setRankOut(logementShard.getRank());
		indexationResult.setLongementIdOut(logementShard.getLongementId());
		indexationResult.setLogementShardSizeOut(logementShard.getLogementShardSize());
		indexationResult.setShardOut(logementShard.getShard());
		indexationResult.setCodeIataOut(logementShard.getCodeIata());
		

		try {
			ResultSetExtractorDocIndexer resultSetExtractorDocIndexer = new ResultSetExtractorDocIndexer(
					transportClient, logementShard, logementShard.getConfigProperties() );

			MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();

			mapSqlParameterSource.addValue("isLogementIdProvided",
					logementShard.getLongementId() != null ? Boolean.TRUE
							: Boolean.FALSE);
			mapSqlParameterSource.addValue("isCodeIataProvided", logementShard
					.getCodeIata() != null ? Boolean.TRUE : Boolean.FALSE);
			mapSqlParameterSource.addValue("logementId", logementShard
					.getLongementId() != null ? logementShard.getLongementId()
					: Long.parseLong("0"));
			mapSqlParameterSource.addValue("codeIata", logementShard
					.getCodeIata() != null ? logementShard.getCodeIata()
					: "PAS_FOURNIS");
			if (logementShard.getLongementId() == null
					&& logementShard.getCodeIata() == null) {
				mapSqlParameterSource.addValue("isThereAFilter", Boolean.FALSE);
			} else {
				mapSqlParameterSource.addValue("isThereAFilter", Boolean.TRUE);
			}
			Integer tailleTotale = logementShard.getLogementShardSize();
			// mapSqlParameterSource.addValue("indexationPacketSize",indexationPacketSize);
			int indexationPacketSize=Integer.parseInt((String)logementShard.getConfigProperties().get("indexation.packet.size")) ;
			int nbrIeteration = tailleTotale /  indexationPacketSize + 1;
			for (int i = 0; i < nbrIeteration; i++) {
				// long
				// indexEnd=((i+1)*indexationPacketSize)>tailleTotale?tailleTotale:(i+1)*indexationPacketSize;
				mapSqlParameterSource.addValue("indexStart", i
						* indexationPacketSize);
				mapSqlParameterSource.addValue("indexationPacketSize",
						indexationPacketSize);
				mapSqlParameterSource.addValue("startDbFoSnapshotTimestamp",
						logementShard.getStartDbFoSnapshotTimestamp());
				mapSqlParameterSource.addValue("endDbFoSnapshotTimestamp",
						logementShard.getEndDbFoSnapshotTimestamp());
				
				BulkResponse bulkResponse = namedParameterJdbcTemplate.query(slqQuery,
						mapSqlParameterSource, resultSetExtractorDocIndexer);
				
				BulkItemResponse[] bulkItems = bulkResponse.getItems();
				for (int j = 0; j < bulkItems.length; j++) {
					if(!bulkItems[j].isFailed()){
						indexationResult.setInsertedDateNbrWithoutFailure(indexationResult.getInsertedDateNbrWithoutFailure() + 1);
					}else{
						LOGGER.error("!!!!!!!!!!!!!!! indexation of the docs whose Id is: '" + bulkItems[j].getFailure().getId() + "' failed. Message: '" + bulkItems[j].getFailure().getMessage() + "' ");
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
		return indexationResult;
	}

	
//	#		mapSqlParameterSource.addValue("shardNumber", configProperties.get("shard.number") );
//	#		mapSqlParameterSource.addValue("shardSliceNumber", configProperties.get("shard.slice.number") );
	
	// private ElasticSearchNode elasticSearchNode;
//	private Properties configProperties;
//	private Integer indexationPacketSize;
	private String jsonIndexeMapping;
//	private Integer shardNumber;
//	private Integer numberOfReplicas;
	private TransportClient transportClient;
	private EsClientNode esClientNode;
	private String slqQuery;
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private JdbcTemplate esVirageMetricsDataSourceSimpleJdbcTemplate;
	private NamedParameterJdbcTemplate esVirageMetricsDataSourceNamedParameterJdbcTemplate;
	private SimpleJdbcInsert insertMetric;

	public void setDataSource(DataSource dataSource) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
				dataSource);
	}

	public void setEsVirageMetricsDataSource(DataSource dataSource) {
		this.esVirageMetricsDataSourceSimpleJdbcTemplate = new JdbcTemplate(
				dataSource);
		this.esVirageMetricsDataSourceNamedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
				dataSource);
		this.insertMetric = new SimpleJdbcInsert(dataSource).withTableName(
				"metrics").usingGeneratedKeyColumns("id");
	}

	public String getSlqQuery() {
		return slqQuery;
	}

	public void setSlqQuery(String slqQuery) {
		this.slqQuery = slqQuery;
	}


	public EsClientNode getEsClientNode() {
		return esClientNode;
	}

	public void setEsClientNode(EsClientNode esClientNode) {
		this.esClientNode = esClientNode;
	}

	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public TransportClient getTransportClient() {
		return transportClient;
	}

	public void setTransportClient(TransportClient transportClient) {
		this.transportClient = transportClient;
	}

//	public Integer getShardNumber() {
//		return shardNumber;
//	}
//
//	public void setShardNumber(Integer shardNumber) {
//		this.shardNumber = shardNumber;
//	}

//	public Integer getNumberOfReplicas() {
//		return numberOfReplicas;
//	}
//
//	public void setNumberOfReplicas(Integer numberOfReplicas) {
//		this.numberOfReplicas = numberOfReplicas;
//	}

	public String getJsonIndexeMapping() {
		return jsonIndexeMapping;
	}

	public void setJsonIndexeMapping(String jsonIndexeMapping) {
		this.jsonIndexeMapping = jsonIndexeMapping;
	}

//	public Integer getIndexationPacketSize() {
//		return indexationPacketSize;
//	}
//
//	public void setIndexationPacketSize(Integer indexationPacketSize) {
//		this.indexationPacketSize = indexationPacketSize;
//	}

//	public Properties getConfigProperties() {
//		return configProperties;
//	}
//
//	public void setConfigProperties(Properties configProperties) {
//		this.configProperties = configProperties;
//	}

}
