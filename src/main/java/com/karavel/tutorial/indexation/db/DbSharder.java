package com.karavel.tutorial.indexation.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.sql.DataSource;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.karavel.front.send.request.model.ResponseOut;
import com.karavel.front.send.request.strategy.IRequestSender;
import com.karavel.front.send.request.strategy.impl.RequestSenderImpl;
import com.karavel.tutorial.indexation.elasticsearch.EsAdminManager;
import com.karavel.tutorial.indexation.elasticsearch.IndexationResult;

public class DbSharder {
	private static final class LogementShardMapper implements RowMapper<LogementShard> {
		
		
		
	    public LogementShard mapRow(ResultSet resultSet, int rowNum) throws SQLException {
			LogementShard logementShard = new LogementShard();
			logementShard
					.setLogementShardSize(resultSet
							.getInt("nbr"));
			logementShard.setLongementId(resultSet
					.getLong("logementId"));
			logementShard.setRank(resultSet
					.getLong("rank"));
			logementShard.setCodeIata(resultSet
					.getString("codeIata"));
			logementShard.setShard(resultSet
					.getInt("shard"));
			return logementShard;
	        
	    }        
	}
	/*
	 * Max time spent to treat a packet	
	 */
	public static int CAMEL_FUTUR_TIMEOUT_SECOND=3600;
	
	private Logger LOGGER = LogManager.getLogger(DbSharder.class);
	
	private String shardSlqQuery;
	private String sqlQueryCountContent;
	private EsAdminManager esAdminManager;
	
	private IRequestSender<IndexationResult> requestSenderAsynchro;

	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public void setDataSource(DataSource dataSource) {
		this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(
				dataSource);
	}

	public String getShardSlqQuery() {
		return shardSlqQuery;
	}

	public void setShardSlqQuery(String shardSlqQuery) {
		this.shardSlqQuery = shardSlqQuery;
	}

	@SuppressWarnings("unchecked")
	public ResponseOut<IndexationResult>[] retrieveLogementShard(	Date startDbFoSnapshotTimestamp
			,Date endDbFoSnapshotTimestamp, String indexName, String aliasName, boolean isDeltaBatch, HashMap<String, Object> configProperties) throws Exception {
		
		
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		mapSqlParameterSource.addValue("shardNumber", Integer.parseInt((String)configProperties.get("shard.number")) );
		mapSqlParameterSource.addValue("shardSliceNumber", Integer.parseInt((String)configProperties.get("shard.slice.number")) );
		mapSqlParameterSource.addValue("startDbFoSnapshotTimestamp", startDbFoSnapshotTimestamp);
		mapSqlParameterSource.addValue("endDbFoSnapshotTimestamp", endDbFoSnapshotTimestamp);
		
		
		 List<LogementShard> logementShards=null;
		 Boolean withSharding=Boolean.parseBoolean((String)configProperties.get("withSharding"));
		 if(withSharding){
				logementShards = (List<LogementShard>) namedParameterJdbcTemplate
				.query(shardSlqQuery, mapSqlParameterSource,
						new LogementShardMapper());
		 }else{
				logementShards= new ArrayList<LogementShard>();
				LogementShard aLogementShard = new LogementShard();
				int tailleTotal=countNumberOfDocs(startDbFoSnapshotTimestamp, endDbFoSnapshotTimestamp, indexName, aliasName, isDeltaBatch, configProperties);
				aLogementShard.setLogementShardSize(tailleTotal);
				aLogementShard.setShard(1);				
				logementShards.add(aLogementShard);
		 }
			
			
			// persister la distribution
			if(!isDeltaBatch){
				esAdminManager.persisterDistribution(logementShards);
			}
	
			int i=0;
			ResponseOut<IndexationResult>[] futureOut =new ResponseOut[logementShards.size()] ;
			for (LogementShard logementShard : logementShards) {
				logementShard.setStartDbFoSnapshotTimestamp(startDbFoSnapshotTimestamp);
				logementShard.setEndDbFoSnapshotTimestamp(endDbFoSnapshotTimestamp);
				logementShard.setAliasName(aliasName);
				logementShard.setIndexName(indexName);
				logementShard.setDeltaBatch(isDeltaBatch);
				logementShard.setConfigProperties(configProperties);
				String route="seda:indexerWorkQueue?concurrentConsumers="+ configProperties.get("concurrentConsumers.number") + "&timeout=300000";
				ResponseOut<IndexationResult> responseOut = requestSenderAsynchro.sendRequest(route, logementShard, IndexationResult.class);
				futureOut[i] = responseOut;
				i++;
			}
			
			for (int index = 0; index < futureOut.length; index++) {
				ResponseOut<IndexationResult> out = (ResponseOut<IndexationResult>) futureOut[index];
				IndexationResult indexationResult = out.get(CAMEL_FUTUR_TIMEOUT_SECOND);
				if(LOGGER.isDebugEnabled()){
					LOGGER.debug("SubmittedDateNbr='"+indexationResult.getSubmittedDateNbr() + "' InsertedDateNbrWithoutFailure='" + indexationResult.getInsertedDateNbrWithoutFailure() +"' ");
				}
			}			
		 return futureOut;
	}

	
	public int countNumberOfDocs(	Date startDbFoSnapshotTimestamp
			,Date endDbFoSnapshotTimestamp, String indexName, String aliasName, boolean isDeltaBatch, HashMap<String, Object> configProperties) {
		MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
		
		mapSqlParameterSource.addValue("startDbFoSnapshotTimestamp", startDbFoSnapshotTimestamp);
		mapSqlParameterSource.addValue("endDbFoSnapshotTimestamp", endDbFoSnapshotTimestamp);
		
		mapSqlParameterSource.addValue("isLogementIdProvided", Boolean.FALSE);
		mapSqlParameterSource.addValue("isCodeIataProvided", Boolean.FALSE);
		mapSqlParameterSource.addValue("logementId", Long.parseLong("0"));
		mapSqlParameterSource.addValue("codeIata","PAS_FOURNIS");
		
	    return this.namedParameterJdbcTemplate.queryForInt(sqlQueryCountContent, mapSqlParameterSource);
	}


	public NamedParameterJdbcTemplate getNamedParameterJdbcTemplate() {
		return namedParameterJdbcTemplate;
	}

	public void setNamedParameterJdbcTemplate(
			NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
		this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
	}

	public IRequestSender<IndexationResult> getRequestSenderAsynchro() {
		return requestSenderAsynchro;
	}

	public void setRequestSenderAsynchro(
			IRequestSender<IndexationResult> requestSenderAsynchro) {
		this.requestSenderAsynchro = requestSenderAsynchro;
	}


	
	public String getSqlQueryCountContent() {
		return sqlQueryCountContent;
	}

	public void setSqlQueryCountContent(String sqlQueryCountContent) {
		this.sqlQueryCountContent = sqlQueryCountContent;
	}

	public EsAdminManager getEsAdminManager() {
		return esAdminManager;
	}

	public void setEsAdminManager(EsAdminManager esAdminManager) {
		this.esAdminManager = esAdminManager;
	}

}
