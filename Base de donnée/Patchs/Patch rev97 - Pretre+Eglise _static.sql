-- CELLID TELEPORTATION
INSERT INTO `ancestra_static`.`scripted_cells` VALUES ('494', '368', '0', '1', '2019,366', '-1');
INSERT INTO `ancestra_static`.`scripted_cells` VALUES ('2019', '380', '0', '1', '494,382', '-1');
INSERT INTO `ancestra_static`.`scripted_cells` VALUES ('2019', '395', '0', '1', '494,382', '-1');

-- CELLID ACTION
INSERT INTO `ancestra_static`.`scripted_cells` VALUES ('2019', '297', '101', '1', '0', '');
INSERT INTO `ancestra_static`.`scripted_cells` VALUES ('2019', '282', '101', '1', '0', '');

-- PRETRE
INSERT INTO `ancestra_static`.`npc_template` (`id`, `bonusValue`, `gfxID`, `scaleX`, `scaleY`, `sex`, `color1`, `color2`, `color3`, `accessories`, `extraClip`, `customArtWork`, `initQuestion`, `ventes`) VALUES ('163', '0', '9000', '100', '100', '0', '-1', '-1', '-1', '0,0,0,0', '-1', '0', '613', '');
UPDATE `ancestra_static`.`npc_questions` SET `responses` = '560;559;518' WHERE `npc_questions`.`ID` =613 LIMIT 1;
INSERT INTO `ancestra_static`.`npcs` (`mapid`, `npcid`, `cellid`, `orientation`) VALUES ('2019', '163', '283', '3');
INSERT INTO `ancestra_static`.`npc_reponses_actions` (`ID`, `type`, `args`) VALUES ('518', '102', '');
UPDATE `ancestra_static`.`npc_questions` SET `cond` = 'isMarried<1', `ifFalse` = '614' WHERE `npc_questions`.`ID` =613 LIMIT 1;
INSERT INTO `ancestra_static`.`npc_questions` (`ID`, `responses`, `params`, `cond`, `ifFalse`) VALUES ('614', '2582;560;559', '', '', '0');
INSERT INTO `ancestra_static`.`npc_reponses_actions` (
`ID` ,
`type` ,
`args`
)
VALUES (
'2582', '1', '2951'
);
INSERT INTO `ancestra_static`.`npc_questions` (`ID`, `responses`, `params`, `cond`, `ifFalse`) VALUES ('2951', '2591;2592', '', '', '0');
INSERT INTO `ancestra_static`.`npc_reponses_actions` (`ID`, `type`, `args`) VALUES ('2592', '1', 'DV');
INSERT INTO `ancestra_static`.`npc_reponses_actions` (`ID`, `type`, `args`) VALUES ('2591', '103', '');
