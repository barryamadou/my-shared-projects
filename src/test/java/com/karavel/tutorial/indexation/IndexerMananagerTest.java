package com.karavel.tutorial.indexation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Resource;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.elasticsearch.action.admin.cluster.settings.ClusterUpdateSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.UpdateSettingsResponse;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.servlet.view.freemarker.FreeMarkerConfigurer;

import com.karavel.commons.mail.SendMailInterface;
import com.karavel.commons.mail.SendMessageIn;
import com.karavel.commons.mail.address.MailAddress;
import com.karavel.commons.mail.content.Content;
import com.karavel.commons.mail.content.FreemarkerTemplateContentStrategyImpl;
import com.karavel.commons.mail.content.StaticContentStrategyImpl;
import com.karavel.front.send.request.model.ResponseOut;
import com.karavel.tutorial.conf.SmtpHeaderWelcomeNewMemberStrategy;
import com.karavel.tutorial.indexation.db.DbSharder;
import com.karavel.tutorial.indexation.elasticsearch.EsAdminManager;
import com.karavel.tutorial.indexation.elasticsearch.IndexationResult;

/**
 * 
 * @author ekhelifasenoussi
 * 
 */

public class IndexerMananagerTest extends CamelSpringTestSupport {


	@Test
	@Ignore
	public void testRetriveLastLaunch() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		esAdminManager.retriveLastLaunch();
		}
	@Test
	public void testCreateViragePackageAlias() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String indexNameToRedirectTo="idx1_0";
		String indexNameToRemoveAliasFrom="idx1_1";
		String aliasName="idx1";
		esAdminManager.rotateAliasNames(aliasName,indexNameToRedirectTo, indexNameToRemoveAliasFrom) ;
		}
	
	@Test
	@Ignore
	public void testDoesAliasExist() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String aliasName="monAlias1";
		esAdminManager.doesAliasExist(aliasName) ;
		}
	
	@Test
	public void testAddAliasToIndex() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String aliasName="idx1";
		String indexName="idx1_0";
		esAdminManager.addAliasToIndex(indexName, aliasName);
		}
	
	
	@Test
	@Ignore
	public void testDoesIndexExists() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String indexName="monAlias1";
		boolean res = esAdminManager.doesIndexExists(indexName) ;
		System.out.println("testDoesIndexEsxists: " + res);
		}
	
	@Test
	@Ignore
	public void testCloseIndex() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String indexName="logementindex0";
		boolean res = esAdminManager.closeIndex(indexName) ;
		System.out.println("testCloseIndex: " + res);
		}
	
	@Test
	@Ignore
	public void testRotationGetNewIndexName() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String indexName="idx1_1";
		String res = esAdminManager.rotationGetNewIndexName(indexName) ;
		System.out.println("new name: " + res);
		}
	//

//

	@Test

	public void testCreateIndex() throws Exception{
		HashMap<String, Object> map = getAndOverWriteConfig(null);
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		boolean deleteAndCreate=true;
		String indexName="idx1_0";
		String aliasName="idx1_0";
		boolean isDeltaBatch=false;
		esAdminManager.createIndexAndMapping(deleteAndCreate, indexName,  isDeltaBatch,map) ;
		}
	@Test
	@Ignore
	public void testIndexStats() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		String indexName="idx1";
		esAdminManager.indexStats(indexName);
		}
	
	
	
	
	@Test
	public void testUpdateNumberOfReplica() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		 String indexName="idx1";
		esAdminManager.updateNumberOfReplica(0, indexName);
	}
	
	
	@Test
	public void testUpdateIndexeSetting() throws Exception{
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		HashMap<String, Object> map=new HashMap<String, Object>();
//		map.put("index.routing.allocation.total_shards_per_node", "1");
//		map.put("index.number_of_replicas", "0");
		map.put("index.number_of_replicas", "1");
		
		
		 String indexName="idx1";
		UpdateSettingsResponse res = esAdminManager.updateIndexeSetting(map, indexName);
		Map<String, Object> heads = res.getHeaders();
//		for (String key : heads.keySet()) {
//			System.out.println(key + ": " + heads.get(key));
//		}
		
		}
	
	
	@Test
	@Ignore
	public void testCreateUserWithCamelAsynchro() throws Exception {

		long startIndexation = System.currentTimeMillis();
		Hashtable<String, Object> meticsInfos=new Hashtable<String, Object>();
		// create index mapping
		EsAdminManager esAdminManager=(EsAdminManager)applicationContext.getBean("esAdminManager");
		boolean deleteAndCreate=true;
		String indexName="idx1";
		String aliasName=null;
		boolean isDeltaBatch=false;
		HashMap<String, Object> map = getAndOverWriteConfig(null);
		esAdminManager.createIndexAndMapping(deleteAndCreate, indexName,  isDeltaBatch, map);
		
		// index docs using a channel
		int i=0;
		DbSharder dbSharder=(DbSharder)applicationContext.getBean("dbSharder");
		SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy/MM/dd'T'HHmmssZ");

		Date startDbFoSnapshotTimestamp=simpleDateFormat.parse("1975/01/01'T'000000");
		Date endDbFoSnapshotTimestamp=simpleDateFormat.parse("2014/02/20'T'000000");
		
		ResponseOut<IndexationResult>[] logementShards = dbSharder.retrieveLogementShard(startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, indexName, aliasName, isDeltaBatch, map);

		// retrieve indexation stats
		meticsInfos=esAdminManager.indexStats(indexName);
		
		// insert stats
		long endIndexation = System.currentTimeMillis();
		meticsInfos.put("indexStartDate", new Date(startIndexation));
		meticsInfos.put("indexEndDate", new Date(endIndexation));
		esAdminManager.insertMetrics(meticsInfos);
		
		// end process indexation
		System.out.println("*** All indexation processes finishes IN " + (endIndexation - startIndexation) + " ms with camel");

		
	}
	

	@Override
	protected AbstractApplicationContext createApplicationContext() {
		 AbstractApplicationContext applicationContext =  new ClassPathXmlApplicationContext(
					"/META-INF/camel/camel-extension.route.xml"
				 ,"/META-INF/camel/river-beans.xml"
				 , "/META-INF/camel/mail/sendmailinterface.xml"
				 , "/META-INF/camel/river.datasource.xml"
		 );
		return applicationContext;
	}
	
	private HashMap<String, Object> getAndOverWriteConfig(MultivaluedMap<String, String> inputParameter){
		HashMap<String, Object> usedParams=new HashMap<String, Object>();
		Properties configProperties=(Properties)applicationContext.getBean("configProperties");
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
	
	@Test
	@Ignore
	public void testSendMessage_onlyPlainText() throws InterruptedException {
		FreeMarkerConfigurer freeMarkerConfigurer=(FreeMarkerConfigurer)applicationContext.getBean("freemarker.configurer");
		SendMailInterface sendMailInterface=(SendMailInterface)applicationContext.getBean("sendNoSpamService");
		//
		sendMailInterface.sendMessage(
				new SendMessageIn(
					new MailAddress("amadou.barry7@gmail.com", "A.GMAIL"), 
					new MailAddress[] {new MailAddress("abarry@karavel.com")}, 
					null, 
					null, 
					new Content(
						new StaticContentStrategyImpl("TEST MAIL : only plain text"),
						new FreemarkerTemplateContentStrategyImpl(
						"mailTemplate.plaintext.ftl", 
						TEMPLATE_MODEL, 
						
						freeMarkerConfigurer.getConfiguration()),	
						null),
					null,
					null,
					"FR",
					"UTF8",
					new SmtpHeaderWelcomeNewMemberStrategy("BVILLAUMIE", "PROMOVACANCES")
				)
			);
	}
	private final static Map<String, Object> TEMPLATE_MODEL = new HashMap<String, Object>();
	static {
		TEMPLATE_MODEL.put("userName", "Dupont");
		TEMPLATE_MODEL.put("emailAddress", "dupont@test.fr");
	}
	private FreeMarkerConfigurer freemarkerConfigurer;

	public FreeMarkerConfigurer getFreemarkerConfigurer() {
		return freemarkerConfigurer;
	}
	@Resource
	public void setFreemarkerConfigurer(FreeMarkerConfigurer freemarkerConfigurer) {
		this.freemarkerConfigurer = freemarkerConfigurer;
	}
}
