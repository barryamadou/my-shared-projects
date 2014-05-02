package com.karavel.tutorial.rest;

import java.io.Serializable;

public class IndexerMananagerIn implements Serializable {
	private static final long serialVersionUID = 1L;
	private String user;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
