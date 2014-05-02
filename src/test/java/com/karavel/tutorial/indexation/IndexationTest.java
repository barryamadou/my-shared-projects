package com.karavel.tutorial.indexation;

import org.apache.camel.test.junit4.CamelSpringTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.karavel.tutorial.indexation.IndexerMananager;
import com.karavel.tutorial.indexation.elasticsearch.ResultSetExtractorDocIndexer;

public class IndexationTest extends CamelSpringTestSupport {
	
	
	@Test
	@Ignore
	public void testStartIndexationFull() throws Exception{
		IndexerMananager indexerMananager=(IndexerMananager)applicationContext.getBean("indexerMananager");
		String user="manuel";
		indexerMananager.startIndexationFull(null);
	}
	@Test
	public void testStartIndexationFullSendMail() throws Exception{
		IndexerMananager indexerMananager=(IndexerMananager)applicationContext.getBean("indexerMananager");
		String user="manuel";
		indexerMananager.startIndexationFull(null);
	}
	
	@Test
	@Ignore
	public void testStartIndexationDeltaSendMail() throws Exception{
		IndexerMananager indexerMananager=(IndexerMananager)applicationContext.getBean("indexerMananager");
		String user="manuel";
		indexerMananager.startIndexationDelta(null);
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

	

}
