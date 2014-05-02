package com.karavel.tutorial.indexation.elasticsearch;

import org.apache.log4j.Logger;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

public class EsClientNode {
	private static final Logger LOGGER = Logger.getLogger(EsClientNode.class);

	private String[] elasticSearchNodes;
	private String elasticsearchclusterName;
	private TransportClient client;
	private Integer elasticSearchNodesPort;

	public void init() throws Exception {
		long start = System.currentTimeMillis();
		Settings settings = ImmutableSettings.settingsBuilder()
				.put("cluster.name", elasticsearchclusterName).build();
		TransportClient client = new TransportClient(settings);
		for (String esNodes : elasticSearchNodes) {
			client.addTransportAddress(new InetSocketTransportAddress(esNodes,
					elasticSearchNodesPort));
		}

		long end = System.currentTimeMillis();
		long nodeInitTime = end - start;
		this.client = client;
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("nodeInitTime Time ms: " + nodeInitTime);
		}

	}

	public void destroy() throws Exception {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("es client shutdown");
		}
	}

	public String getElasticsearchclusterName() {
		return elasticsearchclusterName;
	}

	public void setElasticsearchclusterName(String elasticsearchclusterName) {
		this.elasticsearchclusterName = elasticsearchclusterName;
	}

	public String[] getElasticSearchNodes() {
		return elasticSearchNodes;
	}

	public void setElasticSearchNodes(String[] elasticSearchNodes) {
		this.elasticSearchNodes = elasticSearchNodes;
	}

	public TransportClient getClient() {
		return client;
	}

	public void setClient(TransportClient client) {
		this.client = client;
	}

	public Integer getElasticSearchNodesPort() {
		return elasticSearchNodesPort;
	}

	public void setElasticSearchNodesPort(Integer elasticSearchNodesPort) {
		this.elasticSearchNodesPort = elasticSearchNodesPort;
	}

}
