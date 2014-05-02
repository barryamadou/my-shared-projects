select (@rownum := @rownum + 1) as rank, codeIata, logementId, nbr,  MOD(@rownum, :shardNumber ) as shard
from (
	select  L.IATA_VILLE_DESTINATION as codeIata, L.ID as logementId,  count(1) as nbr
	FROM PACKAGE AS P
	INNER JOIN LOGEMENT as L on L.ID=P.RID_LOGEMENT
	INNER JOIN PACKAGE_CALENDRIER as PC on PC.RID_PACKAGE = P.ID
	WHERE 1=1
	and	PC.DISPO=1 
   AND ( 
			 	( 
                                      PC.UPDATE_DATE >    :startDbFoSnapshotTimestamp   
                           and        PC.UPDATE_DATE <= :endDbFoSnapshotTimestamp
				) 
   ) 
	group by L.IATA_VILLE_DESTINATION, L.ID
	order by  nbr desc
) pourCompter, (SELECT @rownum := 0) r
limit :shardSliceNumber



