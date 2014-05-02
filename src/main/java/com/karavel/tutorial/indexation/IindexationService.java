package com.karavel.tutorial.indexation;

import java.util.Date;
import java.util.HashMap;

import com.karavel.tutorial.indexation.db.LogementShard;
import com.karavel.tutorial.indexation.elasticsearch.IndexationResult;



/**
 * Interface pour creer les users
 * 
 * @author ekhelifasenoussi
 * 
 */
public interface IindexationService {

	IndexationResult indexDocs(LogementShard logementShard
);
}
