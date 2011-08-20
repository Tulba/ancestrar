ALTER TABLE `personnages` ADD `seeSeller` TINYINT( 4 ) NOT NULL AFTER `seeAlign`;
ALTER TABLE `personnages` CHANGE `seeSeller` `seeSeller` TINYINT( 4 ) NOT NULL DEFAULT '0';
ALTER TABLE `personnages` CHANGE `seeAlign` `seeAlign` TINYINT( 4 ) NOT NULL DEFAULT '0';
ALTER TABLE `personnages` ADD `storeObjets` TEXT NOT NULL AFTER `objets`;