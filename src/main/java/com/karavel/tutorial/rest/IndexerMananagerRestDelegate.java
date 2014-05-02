package com.karavel.tutorial.rest;

import javax.jws.WebService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.karavel.tutorial.indexation.IndexerMananager;



@WebService(endpointInterface = "com.karavel.tutorial.rest.IndexerMananagerInterface")
@Path("/indexerMananagerInterfaceRestFacade")
public class IndexerMananagerRestDelegate implements
IndexerMananagerInterface {
	
	private IndexerMananager indexerMananager;
	private Logger LOGGER = LogManager.getLogger(IndexerMananagerRestDelegate.class);

	
	@GET
	@Path("/startIndexationFull")
	@Produces(MediaType.APPLICATION_JSON)	
	public IndexerMananagerOut startIndexationFull(@Context UriInfo uriInfo ){
		MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
		IndexerMananagerOut indexerMananagerOut=new IndexerMananagerOut();;
		try {
			indexerMananagerOut = indexerMananager.startIndexationFull(parameters);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			indexerMananagerOut.setMessage(e.getMessage());
			indexerMananagerOut.setResultatCode(-1);
		}
		return indexerMananagerOut;
	}
	
	@GET
	@Path("/startIndexationDelta")
	@Produces(MediaType.APPLICATION_JSON)	
	public IndexerMananagerOut startIndexationDelta(@Context UriInfo uriInfo ){
		MultivaluedMap<String, String> parameters = uriInfo.getQueryParameters();
		IndexerMananagerOut indexerMananagerOut=new IndexerMananagerOut();
		try {
			indexerMananagerOut = indexerMananager.startIndexationDelta(parameters);
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			indexerMananagerOut.setMessage(e.getMessage());
			indexerMananagerOut.setResultatCode(-1);
		}
		
		return indexerMananagerOut;
	}
	

	public IndexerMananager getIndexerMananager() {
		return indexerMananager;
	}

	public void setIndexerMananager(IndexerMananager indexerMananager) {
		this.indexerMananager = indexerMananager;
	}
	
}
