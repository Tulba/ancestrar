ALTER TABLE `monsters` ADD `capturable` INT( 11 ) NOT NULL DEFAULT '1';
ALTER TABLE `maps` ADD `groupmaxsize` INT NOT NULL DEFAULT '6';
ALTER TABLE `npc_questions` ADD `cond` TEXT NOT NULL;
ALTER TABLE `npc_questions` ADD `ifFalse` INT( 11 ) NOT NULL;