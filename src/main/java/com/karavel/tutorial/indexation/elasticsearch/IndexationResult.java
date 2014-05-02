package com.karavel.tutorial.indexation.elasticsearch;

import java.io.Serializable;

/**
 * 
 * @author ekhelifasenoussi
 *
 */
public class IndexationResult implements Serializable {



	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	private Long rankOut;
	private Long longementIdOut;
	private Integer logementShardSizeOut;
	private Integer shardOut;
	private String  codeIataOut;

	
	
	private int submittedDateNbr;
	private int insertedDateNbrWithoutFailure;

	
	public IndexationResult() {
		super();
	}



	public int getSubmittedDateNbr() {
		return submittedDateNbr;
	}

	public void setSubmittedDateNbr(int submittedDateNbr) {
		this.submittedDateNbr = submittedDateNbr;
	}

	public int getInsertedDateNbrWithoutFailure() {
		return insertedDateNbrWithoutFailure;
	}

	public void setInsertedDateNbrWithoutFailure(int insertedDateNbrWithoutFailure) {
		this.insertedDateNbrWithoutFailure = insertedDateNbrWithoutFailure;
	}

	public Long getRankOut() {
		return rankOut;
	}

	public void setRankOut(Long rankOut) {
		this.rankOut = rankOut;
	}

	public Long getLongementIdOut() {
		return longementIdOut;
	}

	public void setLongementIdOut(Long longementIdOut) {
		this.longementIdOut = longementIdOut;
	}

	public Integer getLogementShardSizeOut() {
		return logementShardSizeOut;
	}

	public void setLogementShardSizeOut(Integer logementShardSizeOut) {
		this.logementShardSizeOut = logementShardSizeOut;
	}

	public Integer getShardOut() {
		return shardOut;
	}

	public void setShardOut(Integer shardOut) {
		this.shardOut = shardOut;
	}

	public String getCodeIataOut() {
		return codeIataOut;
	}

	public void setCodeIataOut(String codeIataOut) {
		this.codeIataOut = codeIataOut;
	}
	
	
	
	
	
	
}
