/*Flamiche*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('718', '5', '718,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('718', '9', '350');

/*Libération*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('719', '5', '719,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('719', '9', '368');

/*Foudroiment*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('720', '5', '720,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('720', '9', '369');

/*Brokle*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('731', '5', '731,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('731', '9', '410');

/*Félintion*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('6664', '5', '6664,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('6664', '9', '412');

/*Arakne*/
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('721', '5', '721,-1,0');
INSERT INTO `ancestra_static`.`use_item_actions` (`template`, `type`, `args`) VALUES ('721', '9', '370');

/*Chaferfu*/
UPDATE `ancestra_static`.`use_item_actions` SET `args` = '373' WHERE `use_item_actions`.`template` =9200 AND `use_item_actions`.`type` =9 AND `use_item_actions`.`args` = '373,1' LIMIT 1 ;
UPDATE `ancestra_static`.`use_item_actions` SET `args` = '9200,-1,0' WHERE `use_item_actions`.`template` =9200 AND `use_item_actions`.`type` =5 AND `use_item_actions`.`args` = '373,-1,0' LIMIT 1 ;

UPDATE `ancestra_static`.`npc_template` SET `ventes` = '9254,9252,9251,9251,9249,9248,9247,8977,8975,8972,8971,8917,8545,8439,8438,8437,8436,8343,8342,8320,8307,8156,8143,8142,8139,8135,8073,7927,7926,7924,7918,7908,7557,7511,7510,7509,7312,7311,7310,7309,1568,1569,6884,1570,7807,7808,7809,7810,7811,7812,7813,7814,7815,7816,7817,7818,7819,7820,7821,7822,7823,7824,7825,7826,7827,7828,7829,7830,7831,7832,7833,7834,7835,7836,7837,7838,7839,7840,7841,7842,7843,7844,7845,7846,7847,7848,7849,7850,7851,7852,7853,7854,7855,7856,7857,7858,7859,7860,7861,7862,7863,7864,7865,7866,7867,7868,7869,7870,7871,7872,7873,7874,7875,7876,9582,8693,8677,8561,8211,2076,1711,1728,1748,8071,2074,7911,7892,7891,7714,7706,7705,7704,7703,7524,7523,7522,7520,7519,7518,7415,7414,2075,6895,6894,6718,6717,6716,6604,2077,8155,8154,8000,8151,8153,6978,8087,730,729,728,727,726,725,724,1575,528,683,795,796,797,686,815,816,817,809,811,812,814,806,807,808,810,802,803,804,805,798,799,800,801,718,720,6664,9200' WHERE `id`='816';