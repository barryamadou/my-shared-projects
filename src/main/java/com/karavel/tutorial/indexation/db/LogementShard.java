package com.karavel.tutorial.indexation.db;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class LogementShard implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Long rank;
	private Long longementId;
	private Integer logementShardSize;
	private Integer shard;
	private String  codeIata;
	
	private Date startDbFoSnapshotTimestamp;
	private Date endDbFoSnapshotTimestamp;
	
	private String indexName;
	private String aliasName;
	private boolean isDeltaBatch;
	
	private HashMap<String, Object> configProperties;

	
	public Long getRank() {
		return rank;
	}
	public void setRank(Long rank) {
		this.rank = rank;
	}
	public Long getLongementId() {
		return longementId;
	}
	public void setLongementId(Long longementId) {
		this.longementId = longementId;
	}
	public Integer getLogementShardSize() {
		return logementShardSize;
	}
	public void setLogementShardSize(Integer logementShardSize) {
		this.logementShardSize = logementShardSize;
	}
	public Integer getShard() {
		return shard;
	}
	public void setShard(Integer shard) {
		this.shard = shard;
	}
	public String getCodeIata() {
		return codeIata;
	}
	public void setCodeIata(String codeIata) {
		this.codeIata = codeIata;
	}
	public Date getStartDbFoSnapshotTimestamp() {
		return startDbFoSnapshotTimestamp;
	}
	public void setStartDbFoSnapshotTimestamp(Date startDbFoSnapshotTimestamp) {
		this.startDbFoSnapshotTimestamp = startDbFoSnapshotTimestamp;
	}
	public Date getEndDbFoSnapshotTimestamp() {
		return endDbFoSnapshotTimestamp;
	}
	public void setEndDbFoSnapshotTimestamp(Date endDbFoSnapshotTimestamp) {
		this.endDbFoSnapshotTimestamp = endDbFoSnapshotTimestamp;
	}
	public String getIndexName() {
		return indexName;
	}
	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}
	public String getAliasName() {
		return aliasName;
	}
	public void setAliasName(String aliasName) {
		this.aliasName = aliasName;
	}
	public boolean isDeltaBatch() {
		return isDeltaBatch;
	}
	public void setDeltaBatch(boolean isDeltaBatch) {
		this.isDeltaBatch = isDeltaBatch;
	}
	public HashMap<String, Object> getConfigProperties() {
		return configProperties;
	}
	public void setConfigProperties(HashMap<String, Object> configProperties) {
		this.configProperties = configProperties;
	}

}
