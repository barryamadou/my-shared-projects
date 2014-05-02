
startDbFoSnapshotTimestamp:		${startDbFoSnapshotTimestamp?string("yyyy-MM-dd HH:mm:ss")}
endDbFoSnapshotTimestamp:		${endDbFoSnapshotTimestamp?string("yyyy-MM-dd HH:mm:ss")}
connected.user: 				${connected_user}

<#if nbrTotaIndexed?? >
	nbrTotaIndexed: 					${nbrTotaIndexed}
</#if>
<#if nbrTotaIndexed?? >
	totalIndexedDocs: 					${totalIndexedDocs}
</#if>
<#if nbrTotaIndexed?? >
	indexationTimeSecond: 				${indexationTimeSecond}
</#if>
<#if nbrTotaIndexed?? >
	throuputDocsPerSecond: 				${throuputDocsPerSecond}
</#if>

<#if errorMessage?? >
	errorMessage: 					${errorMessage}
</#if>


Pour voir l'état de l'indexe: 	http://esviragefo-kibana.in.karavel.com/index.html#dashboard/elasticsearch/NbrDatesParVilleDepartArrivee
Pour voir les dashbords: 		https://splunk-head01.in.karavel.com:8000/en-US/app/search/virage__elastic_search (login windows)


base de données source:				${virageFO_driver_url}
ClusterDestination:					${es_clusterName}
elasticSearchNodes.names:			${elasticSearchNodes_names}

