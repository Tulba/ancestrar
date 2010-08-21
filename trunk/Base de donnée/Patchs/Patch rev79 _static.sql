INSERT INTO `ancestra_static`.`npc_template` (`id`, `bonusValue`, `gfxID`, `scaleX`, `scaleY`, `sex`, `color1`, `color2`, `color3`, `accessories`, `extraClip`, `customArtWork`, `initQuestion`, `ventes`) VALUES ('1', '0', '6000', '100', '100', '1', '-1', '-1', '-1', '0,0,0,0', '-1', '0', '-1', '');

/*Dofus emeraude*/
UPDATE `ancestra_static`.`item_template` SET `statsTemplate` = '7D#33#64#0#1d50+50' WHERE `item_template`.`id` =737 AND `item_template`.`type` =23 AND `item_template`.`name` = 'Dofus Emeraude' AND `item_template`.`level` =6 AND `item_template`.`statsTemplate` = '7D#96#12C#0#1d50+50' AND `item_template`.`pod` =5 AND `item_template`.`panoplie` = -1 AND `item_template`.`prix` =500000 AND `item_template`.`condition` = '' AND `item_template`.`armesInfos` = '' LIMIT 1 ;
