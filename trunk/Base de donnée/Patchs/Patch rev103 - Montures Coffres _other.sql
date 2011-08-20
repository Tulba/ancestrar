ALTER TABLE `accounts` DROP `stable`;
TRUNCATE TABLE `mounts_data`;

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for `coffres`
-- ----------------------------
DROP TABLE IF EXISTS `coffres`;
CREATE TABLE `coffres` (
  `id` int(11) NOT NULL,
  `id_house` int(11) NOT NULL,
  `mapid` int(11) NOT NULL,
  `cellid` int(11) NOT NULL,
  `object` text NOT NULL,
  `kamas` int(11) NOT NULL,
  `key` varchar(8) NOT NULL DEFAULT '-',
  `owner_id` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of coffres
-- ----------------------------
INSERT INTO coffres VALUES ('1', '655', '7710', '107', '', '0', '-', '0');
INSERT INTO coffres VALUES ('2', '645', '7701', '156', '', '0', '-', '0');
INSERT INTO coffres VALUES ('3', '645', '7703', '166', '', '0', '-', '0');
INSERT INTO coffres VALUES ('4', '700', '7694', '88', '', '0', '-', '0');
INSERT INTO coffres VALUES ('5', '701', '7696', '107', '', '0', '-', '0');
INSERT INTO coffres VALUES ('6', '684', '7686', '156', '', '0', '-', '0');
INSERT INTO coffres VALUES ('7', '684', '7687', '166', '', '0', '-', '0');
INSERT INTO coffres VALUES ('8', '641', '7617', '107', '', '0', '-', '0');
INSERT INTO coffres VALUES ('9', '652', '7636', '154', '', '0', '-', '0');
INSERT INTO coffres VALUES ('10', '674', '7741', '156', '', '0', '-', '0');
INSERT INTO coffres VALUES ('11', '674', '7740', '166', '', '0', '-', '0');
INSERT INTO coffres VALUES ('13', '690', '7682', '88', '', '0', '-', '0');
INSERT INTO coffres VALUES ('14', '667', '7661', '156', '', '0', '-', '0');
INSERT INTO coffres VALUES ('15', '667', '7660', '166', '', '0', '-', '0');
INSERT INTO coffres VALUES ('16', '670', '7625', '156', '', '0', '-', '0');
INSERT INTO coffres VALUES ('17', '670', '7624', '166', '', '0', '-', '0');
INSERT INTO coffres VALUES ('18', '693', '7630', '88', '', '0', '-', '0');
INSERT INTO coffres VALUES ('19', '698', '7647', '107', '', '0', '-', '0');
INSERT INTO coffres VALUES ('20', '651', '7729', '107', '', '0', '-', '0');
