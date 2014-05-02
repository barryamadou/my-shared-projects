SELECT 'viragepackage'                                                                           AS _index,
       PC.ID                                                                                     AS _id,
       L.ID                                                                                      AS "logement.logementId", 
	   11111 AS "logement.logementBbqId", 
       L.Nom                                                                                     AS "logement.nom",
       LAG.LATITUDE                                                                              AS "logement.coordonnees.lat",
       LAG.LONGITUDE                                                                             AS "logement.coordonnees.lon",
       LAG.LONGITUDE                                                                             AS "logement.lon",
       LAG.LATITUDE                                                                              AS "logement.lat",
       P.ID                                                                                      AS "produitId",
       P.OFFRE_COMPLETE_ID                                                                       AS "offreCompleteId",
       PC.ID                                                                					 AS "calendrierId",
       IF(P.VILLE_DEPART = L.IATA_VILLE_DESTINATION
           OR ( P.VILLE_DEPART = '999'
                AND L.IATA_VILLE_DESTINATION = 'ZZZ' ), 0, RFD.ID) 								AS "infosGeographiques.villeDepart.id",
       IF(P.VILLE_DEPART = L.IATA_VILLE_DESTINATION
           OR ( P.VILLE_DEPART = '999'
                AND L.IATA_VILLE_DESTINATION = 'ZZZ' ), L.IATA_VILLE_DESTINATION, RFD.CODE_IATA) AS "infosGeographiques.villeDepart.libelle",
       RFA.ID                                                                                    AS "infosGeographiques.villeArrivee.id",
       RFA.CODE_IATA                                                                             AS "infosGeographiques.villeArrivee.libelle",
       L.RID_VILLE_LOGEMENT                                                                      AS "villeLogementId",
       P.DUREE_JOUR                                                                              AS "duree.nombreJours",
       P.DUREE_NUIT                                                                              AS "duree.nombreNuits",
       'Jour'                                                                                    AS "duree.typeDuree",
       RCH.NB_ETOILE                                                                             AS "logement.categorie.niveau",
       RP.ID                                                                                     AS "pension.id",
       RP.LIBELLE                                                                                AS "pension.libelle",
       PC.DATE_DEBUT                                                                             AS "dateDepart",
       PC.DATE_FIN                                                                               AS "dateRetour",
       'formules'                                                                                AS "formules",
       PC.PRIX                                                                                   AS "tarif.prixMinTTC",
       LA.NOTE                                                                                   AS "noteGlobale",
       P.STOP_AFFAIRE                                                                            AS "stopAffaire",
       P.IS_COUP_DE_COEUR                                                                        AS "coupDeCoeur",
       IF(P.RID_TOUROPERATEUR = 0, 424, P.RID_TOUROPERATEUR)                                     AS "fournisseur.id",
       PC.SCORE                                                                                  AS "risqueAerien",
       IF(P.RID_TOUROPERATEUR = 45, 1, 0)                                                        AS "isPromovel",
       PC.DISPO                                                                                  AS "isDispo",
       /*       PT.RID_THEME as "theme.id",       RT.LIBELLE as "theme.libelle", */
       GROUP_CONCAT(DISTINCT PT.RID_THEME SEPARATOR '||')                                        AS "theme.id",
       GROUP_CONCAT(DISTINCT RT.LIBELLE SEPARATOR '||')                                          AS "theme.libelle",
       GROUP_CONCAT(DISTINCT PS.CODE_MARQUE SEPARATOR '||')                                      AS "codeMarque",
       RCH.NB_ETOILE                                                                             AS "logement.categorie.libelle",
       RVL.LIBELLE                                                                               AS "logement.ville.nom"  ,
       RVL.CODE                                                                                  AS "logement.ville.code",
       RVL.CODE_PAYS                                                                             AS "logement.ville.codeIATAPays",
       PC.UPDATE_DATE                                                                            AS "tarif.dateMAJPrix",
	   PC.RISK_VENDEUR 																			 AS "riskVendeur" 
FROM   PACKAGE AS P
       INNER JOIN REF_VILLE RFD
               ON RFD.CODE_IATA = P.VILLE_DEPART
       INNER JOIN REF_PENSION RP
               ON RP.CODE = P.REF_PENSION
       INNER JOIN PACKAGE_CALENDRIER AS PC
               ON PC.RID_PACKAGE = P.ID
       INNER JOIN LOGEMENT AS L
               ON L.ID = P.RID_LOGEMENT
       INNER JOIN REF_VILLE RFA
               ON RFA.CODE_IATA = L.IATA_VILLE_DESTINATION
       INNER JOIN REF_CATEGORIE_HOTEL AS RCH
               ON RCH.CODE = L.NB_ETOILE
       INNER JOIN REF_VILLE_LOGEMENT AS RVL
               ON RVL.ID = L.RID_VILLE_LOGEMENT
       LEFT JOIN LOGEMENT_AVIS AS LA
              ON LA.RID_LOGEMENT = L.ID
                 AND LA.RID_MARQUE = 1
       INNER JOIN PACKAGE_THEME PT
               ON PT.RID_PACKAGE = P.ID
       INNER JOIN REF_THEME RT
               ON RT.ID = PT.RID_THEME
               
       LEFT JOIN LOGEMENT_ADRESSE AS LAG
              ON LAG.RID_LOGEMENT = L.ID
       INNER JOIN PACKAGE_SITE AS PS
               ON PS.RID_PACKAGE = P.ID
WHERE  1=1
		and RFA.CODE_IATA='ZZZ'
		and P.DISPO = 1
       AND PC.DISPO = 1 
       AND (  :isLogementIdProvided=false  or L.ID = :logementId )
       AND ( :isCodeIataProvided=false or RFA.CODE_IATA = :codeIata  )
	   AND ( 
				 	( 
                                          PC.UPDATE_DATE >    :startDbFoSnapshotTimestamp   
                               and        PC.UPDATE_DATE <= :endDbFoSnapshotTimestamp
					) 
	   ) 
       
GROUP  BY PC.ID
LIMIT  :indexStart, :indexationPacketSize 

