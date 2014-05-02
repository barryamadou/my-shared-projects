package com.karavel.tutorial.rest;

import java.util.HashMap;

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;


@WebService
public interface IndexerMananagerInterface {
	
	 @WebMethod
	IndexerMananagerOut startIndexationFull(UriInfo uriInfo);
	 
	 
	 @WebMethod
		public IndexerMananagerOut startIndexationDelta(UriInfo uriInfo);



}



