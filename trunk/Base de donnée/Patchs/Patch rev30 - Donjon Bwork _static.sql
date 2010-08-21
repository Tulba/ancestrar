INSERT INTO `endfight_action` VALUES ('9750', '4', '0', '9751,429', '');
INSERT INTO `endfight_action` VALUES ('9751', '4', '0', '9752,366', '');
INSERT INTO `endfight_action` VALUES ('9752', '4', '0', '9755,351', '');
INSERT INTO `endfight_action` VALUES ('9755', '4', '0', '9757,375', '');
INSERT INTO `endfight_action` VALUES ('9757', '4', '0', '9758,366', '');
INSERT INTO `endfight_action` VALUES ('9758', '4', '0', '9759,366', '');
INSERT INTO `endfight_action` VALUES ('9759', '4', '0', '9760,417', '');
INSERT INTO `endfight_action` VALUES ('9760', '4', '0', '9767,403', '');

INSERT INTO `ancestra_static`.`npc_template` (`id`, `bonusValue`, `gfxID`, `scaleX`, `scaleY`, `sex`, `color1`, `color2`, `color3`, `accessories`, `extraClip`, `customArtWork`, `initQuestion`, `ventes`) VALUES ('775', '0', '1003', '100', '100', '1', '-1', '-1', '-1', '0,0,0,0', '-1', '0', '-1', '');
INSERT INTO `ancestra_static`.`npcs` (`mapid`, `npcid`, `cellid`, `orientation`) VALUES ('9470', '775', '184', '3');
UPDATE `ancestra_static`.`npc_template` SET `initQuestion` =  '3172' WHERE `npc_template`.`id` =775 LIMIT 1 ;

UPDATE `ancestra_static`.`npc_questions` SET `responses` =  '2794' WHERE `npc_questions`.`ID` =3172 AND `npc_questions`.`responses` =  '2794;661' AND `npc_questions`.`params` = '' LIMIT 1 ;
INSERT INTO `ancestra_static`.`npc_reponses_actions` (
`ID` ,
`type` ,
`args`
)
VALUES (
'2794', '15', '9570,351,8135,9470'
);

INSERT INTO `ancestra_static`.`npc_template` (`id`, `bonusValue`, `gfxID`, `scaleX`, `scaleY`, `sex`, `color1`, `color2`, `color3`, `accessories`, `extraClip`, `customArtWork`, `initQuestion`, `ventes`) VALUES ('776', '0', '1012', '100', '100', '1', '-1', '-1', '-1', '0,0,0,0', '-1', '0', '-1', '');


INSERT INTO `ancestra_static`.`npcs` (`mapid`, `npcid`, `cellid`, `orientation`) VALUES ('9767', '776', '281', '5');

UPDATE `ancestra_static`.`npc_template` SET `initQuestion` =  '3173' WHERE `npc_template`.`id` =776 LIMIT 1 ;

UPDATE `ancestra_static`.`npc_questions` SET `responses` =  '2795' WHERE `npc_questions`.`ID` =3173 AND `npc_questions`.`responses` =  '279' AND `npc_questions`.`params` = '' LIMIT 1 ;

INSERT INTO `ancestra_static`.`npc_reponses_actions` (
`ID` ,
`type` ,
`args`
)
VALUES (
'2795', '0', '9470,277'
);
