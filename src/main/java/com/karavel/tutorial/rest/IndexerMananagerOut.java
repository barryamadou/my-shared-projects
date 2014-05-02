package com.karavel.tutorial.rest;

import java.io.Serializable;

public class IndexerMananagerOut implements Serializable {
	private static final long serialVersionUID = 1L;
	private  int resultatCode;
	private String message;
	
	public int getResultatCode() {
		return resultatCode;
	}
	public void setResultatCode(int resultatCode) {
		this.resultatCode = resultatCode;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
