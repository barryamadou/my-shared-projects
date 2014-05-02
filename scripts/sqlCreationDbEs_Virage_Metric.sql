CREATE DATABASE `es_virage_metrics` /*!40100 DEFAULT CHARACTER SET latin1 */;


/* creation table: distribution */
DROP TABLE IF EXISTS `es_virage_metrics`.`distribution`;
CREATE TABLE  `es_virage_metrics`.`distribution` (
  `rank` bigint(20) unsigned DEFAULT '0',
  `codeIata` varchar(45) DEFAULT NULL,
  `logementId` bigint(20) unsigned DEFAULT NULL,
  `nbr` int(10) unsigned DEFAULT NULL,
  `shard` int(10) unsigned DEFAULT NULL,
  `cerationDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB ;

/* creation table: metrics */
DROP TABLE IF EXISTS `es_virage_metrics`.`metrics`;
CREATE TABLE  `es_virage_metrics`.`metrics` (
  /*`id` int(10) unsigned NOT NULL AUTO_INCREMENT, */
  `numberOfReplicas` int(10) unsigned DEFAULT NULL,
  `shardNumber` int(10) unsigned DEFAULT NULL,
  `indexationPacketSize` int(10) unsigned DEFAULT NULL,
  `concurrentConsumersNumber` int(10) unsigned DEFAULT NULL,
  `shardSliceNumber` int(10) unsigned DEFAULT NULL,
  `elasticSearchNodesNames` varchar(45) DEFAULT NULL,
  `totalIndexedDocs` int(10) unsigned DEFAULT NULL,
  `indexationTimeSecond` int(10) unsigned DEFAULT NULL,
  `throuputDocsPerSecond` float NOT NULL,
  `esClusterName` varchar(100) DEFAULT NULL,
  `indexStartDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `indexEndDate` timestamp,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB ;


ALTER TABLE `es_virage_metrics`.`metrics` ADD COLUMN `startDbFoSnapshotTimestamp` TIMESTAMP AFTER `indexEndDate`,
 ADD COLUMN `endDbFoSnapshotTimestamp` TIMESTAMP AFTER `startDbFoSnapshotTimestamp`,
 ADD COLUMN `user` VARCHAR(45) AFTER `endDbFoSnapshotTimestamp`,
 ADD COLUMN `batchType` VARCHAR(45) AFTER `user`;
 
ALTER TABLE `es_virage_metrics`.`metrics` MODIFY COLUMN `elasticSearchNodesNames` VARCHAR(200) ; 
 