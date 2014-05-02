package com.karavel.tutorial.indexation.elasticsearch;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.joda.time.format.DateTimeFormatter;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.StringUtils;

import com.karavel.tutorial.indexation.db.LogementShard;


public class ResultSetExtractorDocIndexer implements ResultSetExtractor<BulkResponse> {
    private static final Logger               LOGGER = Logger.getLogger(ResultSetExtractorDocIndexer.class);
	
	public static String DELIMETER="||";
	private Client client;
	private LogementShard logementShardIn;
	private HashMap<String, Object> configProperties;
	public ResultSetExtractorDocIndexer(Client client, LogementShard logementShardIn, HashMap<String, Object> configProperties 
		
	) {
		this.logementShardIn=logementShardIn;
		this.client=client;
		this.configProperties=configProperties;
		
	}
	
	

	
	@Override
	public BulkResponse extractData(ResultSet resultSet) throws SQLException,
			DataAccessException {
		BulkResponse bulkResponse=null;
		try {
			
			BulkRequestBuilder responseBulkRequestBuilder = client.prepareBulk();
			DateTimeFormatter dateTimeFormatter=ISODateTimeFormat.basicDateTimeNoMillis();
			
			
			
			
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("**************************** Start building object *********************************************");
			}
			long start = System.currentTimeMillis();
				boolean isThereDataToIndexe=false;
				long indexDataSize=0;
				while (resultSet.next()) {
				
					//============================== Multivalue column ========================
					//theme
					String[] themeLibelleSplit=null;
					String[] themeidSplit=null;
					String themeids = resultSet.getString("theme.id");
					String themeLibelles = resultSet.getString("theme.libelle");
					if(StringUtils.hasText(themeids)){
						themeidSplit = StringUtils.tokenizeToStringArray(themeids, DELIMETER);
					}
					if(StringUtils.hasText(themeLibelles)){
						themeLibelleSplit= StringUtils.tokenizeToStringArray(themeLibelles, DELIMETER);
					}
					//codemarque
					String[] codeMarqueSplit=null;
					String codeMarques = resultSet.getString("codeMarque");
					if(StringUtils.hasText(codeMarques)){
						codeMarqueSplit = StringUtils.tokenizeToStringArray(codeMarques, DELIMETER);
					}
					
					
					//============================== Build the object ========================
					
					XContentBuilder jsonSejour;
					jsonSejour = XContentFactory.jsonBuilder().prettyPrint()
							.startObject()
								.field("dateDepart").value(resultSet.getDate("dateDepart"))
								.field("dateRetour").value(resultSet.getDate("dateRetour"))
								.field("calendrierId").value(resultSet.getLong("calendrierId"))
								.field("rank").value(new BigInteger ("1111111111111119995") )
								.field("villeLogementId").value(resultSet.getLong("villeLogementId"))
								.field("risqueAerien").value(resultSet.getInt("risqueAerien"))
								.field("noteGlobale").value(resultSet.getInt("noteGlobale"))
								.field("offreCompleteId").value(resultSet.getLong("offreCompleteId"))
								.field("produitId").value(resultSet.getLong("produitId"))
								.field("formules").value(resultSet.getString("formules"))
								.field("riskVendeur").value(resultSet.getInt("riskVendeur"))
								.startArray("codeMarque");
								if(codeMarqueSplit!=null && codeMarqueSplit.length>0){
									for(int i=0; i<codeMarqueSplit.length; i++ ){
										jsonSejour.value(codeMarqueSplit[i]);
									}
								}
								jsonSejour.endArray()
								.field("isDispo").value(resultSet.getString("isDispo"))
								.field("isPromovel").value(resultSet.getBoolean("isPromovel"))
								.field("coupDeCoeur").value(resultSet.getBoolean("coupDeCoeur"))
								.field("stopAffaire").value(resultSet.getBoolean("stopAffaire"))
								.startObject("duree")
									.field("nombreJours").value(resultSet.getInt("duree.nombreJours"))
									.field("nombreNuits").value(resultSet.getInt("duree.nombreNuits"))
									.field("typeDuree").value(resultSet.getString("duree.typeDuree"))
								.endObject()
								.startObject("pension")
									.field("id").value(resultSet.getInt("pension.id"))
									.field("libelle").value(resultSet.getString("pension.libelle"))
								.endObject()
								.startObject("tarif")
									.field("prixMinTTC").value(resultSet.getInt("tarif.prixMinTTC"))
									.field("dateMAJPrix").value(resultSet.getDate("tarif.dateMAJPrix"))
								.endObject()
								.startObject("fournisseur")
									.field("id").value(resultSet.getInt("fournisseur.id"))
								.endObject()
								.startArray("theme");
									if(themeidSplit!=null && themeLibelleSplit!=null && themeidSplit.length==themeLibelleSplit.length && themeLibelleSplit.length >0){
										for(int i=0; i<themeLibelleSplit.length; i++ ){
											jsonSejour.startObject()
												.field("id").value(new Integer(themeidSplit[i]))
												.field("libelle").value(themeLibelleSplit[i])
											.endObject();
										}
									}
								jsonSejour.endArray()
								.startObject("infosGeographiques")
									.startObject("villeDepart")
										.field("id").value(resultSet.getInt("infosGeographiques.villeDepart.id"))
										.field("libelle").value(resultSet.getString("infosGeographiques.villeDepart.libelle"))
									.endObject()
									.startObject("villeArrivee")
									.field("id").value(resultSet.getInt("infosGeographiques.villeArrivee.id"))
										.field("libelle").value(resultSet.getString("infosGeographiques.villeArrivee.libelle"))
									.endObject()
								.endObject()
								.startObject("logement")
									.field("logementId").value(resultSet.getLong("logement.logementId"))
									.field("logementBbqId").value(resultSet.getLong("logement.logementBbqId"))
									.field("nom").value(resultSet.getString("logement.nom"))
									.startObject("categorie")
										.field("niveau").value(resultSet.getInt("logement.categorie.niveau"))
										.field("libelle").value(resultSet.getString("logement.categorie.libelle"))
									.endObject()
									.startObject("ville")
										.field("nom").value(resultSet.getString("logement.ville.nom"))
										.field("code").value(resultSet.getString("logement.ville.code"))
										.field("codeIATAPays").value(resultSet.getString("logement.ville.codeIATAPays"))
									.endObject()
									.array("lonlat", resultSet.getDouble("logement.lon"), resultSet.getDouble("logement.lat"))
									.array("coordonnees", resultSet.getDouble("logement.lon"), resultSet.getDouble("logement.lat"))
								.endObject()
							.endObject()
							;
	
				    //============================== adding built object to bulk query ========================
					String usedName=logementShardIn.isDeltaBatch()?logementShardIn.getAliasName():logementShardIn.getIndexName();	
					responseBulkRequestBuilder.add(client
							.prepareIndex(usedName, this.configProperties.get("index.type").toString(),
									Long.toString(resultSet.getLong("_id")))
							.setSource(jsonSejour)
							//overwrite the previous values
							.setCreate(false)
							.setRefresh(false)
							.setReplicationType("default")
							.setConsistencyLevel(WriteConsistencyLevel.DEFAULT)
							 .setRouting(logementShardIn.getShard().toString())
//							 .setTimestamp(dateTimeFormatter.print(logementShardIn.getEndDbFoSnapshotTimestamp().getTime()))
//							 .setRouting(Integer.toString(resultSet.getDate("dateDepart").getMonth()))
//							 .setRouting((String)configProperties.get("routingColumn"))
//							 .setRouting(resultSet.getString("logement.logementId"))
							.request());
					isThereDataToIndexe=true;
					indexDataSize++;
				}
				
		    //============================== end build query ========================
			long ends = System.currentTimeMillis();
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("*******************************EXECUTION TIME EN MS  building object : "+ Long.toString(ends-start) + "**********************");
			}
		    //============================== executing query to ES ========================
			if(LOGGER.isInfoEnabled()){
				LOGGER.info("***************************** Start executing query to ES ****************************************");
			}
			 start = System.currentTimeMillis();
			 if(isThereDataToIndexe){
				 bulkResponse = responseBulkRequestBuilder.execute().actionGet(EsAdminManager.ELASTCSEAR_SERVER_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
				 long tookInMillis = bulkResponse.getTookInMillis();
				 double throuput=1000000;
				 if(tookInMillis!=0){
					  throuput=indexDataSize/(tookInMillis/1000.0);
				 }
				 ends = System.currentTimeMillis();
					if(LOGGER.isInfoEnabled()){
						LOGGER.info("****************************** EXECUTION TIME EN MS executing query to ES : "+ Long.toString(ends-start) + "*************************");
						LOGGER.info("****************************** bulkResponse.getTookInMillis() : "+ tookInMillis+ " indexDataSize: " + indexDataSize + " throuput: " + throuput+  " docs/s " + " *************************");
						LOGGER.info("Routing: " + logementShardIn.getShard().toString());
					}
			 }else{
					if(LOGGER.isInfoEnabled()){
						LOGGER.info("***************************** NO DATA TO INDEX IN ES ****************************************");
					}
			 }
		
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}finally{
//			client.close();
//			node.stop();
		}
		return bulkResponse;
	}
	

}
