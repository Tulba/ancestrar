ALTER TABLE `guilds` ADD `capital` INT( 11 ) NOT NULL DEFAULT '0', ADD `nbrmax` INT( 11 ) NOT NULL DEFAULT '0';
ALTER TABLE `guilds` ADD `sorts` VARCHAR( 255 ) NOT NULL DEFAULT '462;0|461;0|460;0|459;0|458;0|457;0|456;0|455;0|454;0|453;0|452;0|451;0|',
ADD `stats` VARCHAR( 255 ) NOT NULL DEFAULT '176;100|158;1000|124;100|';

ALTER TABLE `percepteurs` ADD `kamas` INT( 11 ) NOT NULL ;
ALTER TABLE `percepteurs` ADD `xp` INT( 11 ) NOT NULL ;