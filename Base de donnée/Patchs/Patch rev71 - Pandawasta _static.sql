UPDATE `ancestra_static`.`monsters` SET `spells` = '588@1;1116@1;544@1|544@2;1116@2;588@2|544@3;1116@3;588@3|544@4;1116@4;588@4|544@5;1116@5;588@5|544@6;1116@6;588@6' WHERE `monsters`.`id` =516 LIMIT 1 ;
UPDATE `ancestra_static`.`monsters` SET `stats` = '0,0,1,0,110|0,0,1,0,120|0,0,1,0,130|0,0,1,0,140|0,0,1,0,150|0,0,1,0,175' WHERE `monsters`.`id` =516 LIMIT 1 ;
UPDATE `ancestra_static`.`monsters` SET `pdvs` = '300|325|350|375|400|450' WHERE `monsters`.`id` =516 LIMIT 1 ;
UPDATE `ancestra_static`.`monsters` SET `points` = '6;5|6;5|6;5|6;5|6;5|6;5' WHERE `monsters`.`id` =516 LIMIT 1 ;
UPDATE `ancestra_static`.`monsters` SET `inits` = '1|1|1|1|1|1' WHERE `monsters`.`id` =516 LIMIT 1 ;
UPDATE `ancestra_static`.`monsters` SET `grades` = '1@10;10;10;10;10;10;10|2@15;15;15;15;15;15;15|3@20;20;20;20;20;20;20|4@25;25;25;25;25;25;25|5@30;30;30;30;30;30;30|6@35;35;35;35;35;35;35' WHERE `monsters`.`id` =516 LIMIT 1 ;
DELETE FROM `ancestra_static`.`experience` WHERE `experience`.`lvl` = 39;
INSERT INTO `ancestra_static`.`experience` (
`lvl` ,
`perso` ,
`metier` ,
`dinde` ,
`pvp`
)
VALUES (
'39', '1580000', '34566', '195250', '-1'
);