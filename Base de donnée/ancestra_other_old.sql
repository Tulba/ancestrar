/*
Navicat MySQL Data Transfer

Source Server         : LocalHost
Source Server Version : 50136
Source Host           : localhost:3306
Source Database       : ancestra_other

Target Server Type    : MYSQL
Target Server Version : 50136
File Encoding         : 65001

Date: 2010-06-25 17:47:08
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for `accounts`
-- ----------------------------
DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `guid` int(11) NOT NULL AUTO_INCREMENT,
  `account` varchar(30) NOT NULL,
  `pass` varchar(50) NOT NULL,
  `level` int(11) NOT NULL DEFAULT '0',
  `email` varchar(100) NOT NULL,
  `lastIP` varchar(15) NOT NULL,
  `lastConnectionDate` varchar(100) NOT NULL,
  `question` varchar(100) NOT NULL DEFAULT 'DELETE?',
  `reponse` varchar(100) NOT NULL DEFAULT 'DELETE',
  `pseudo` varchar(30) NOT NULL,
  `banned` tinyint(3) NOT NULL DEFAULT '0',
  `reload_needed` tinyint(1) NOT NULL DEFAULT '1',
  `bankKamas` int(11) NOT NULL DEFAULT '0',
  `bank` text NOT NULL,
  `friends` text NOT NULL,
  `enemy` text NOT NULL,
  `stable` text NOT NULL,
  `points` int(11) NOT NULL DEFAULT '0',
  `logged` int(1) NOT NULL DEFAULT '0',
  `verif` varchar(50) NOT NULL,
  PRIMARY KEY (`guid`),
  UNIQUE KEY `account` (`account`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of accounts
-- ----------------------------

-- ----------------------------
-- Table structure for `guilds`
-- ----------------------------
DROP TABLE IF EXISTS `guilds`;
CREATE TABLE `guilds` (
  `id` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `emblem` varchar(20) NOT NULL,
  `lvl` int(11) NOT NULL DEFAULT '1',
  `xp` bigint(20) NOT NULL DEFAULT '0',
  KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of guilds
-- ----------------------------

-- ----------------------------
-- Table structure for `guild_members`
-- ----------------------------
DROP TABLE IF EXISTS `guild_members`;
CREATE TABLE `guild_members` (
  `guid` int(11) NOT NULL,
  `guild` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `level` int(11) NOT NULL,
  `gfxid` int(11) NOT NULL,
  `rank` int(11) NOT NULL,
  `xpdone` bigint(20) NOT NULL,
  `pxp` int(11) NOT NULL,
  `rights` int(11) NOT NULL,
  `align` tinyint(4) NOT NULL,
  `lastConnection` varchar(30) NOT NULL,
  UNIQUE KEY `guid` (`guid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of guild_members
-- ----------------------------

-- ----------------------------
-- Table structure for `items`
-- ----------------------------
DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `guid` int(11) NOT NULL,
  `template` int(11) NOT NULL,
  `qua` int(11) NOT NULL,
  `pos` int(11) NOT NULL,
  `stats` text NOT NULL,
  UNIQUE KEY `guid` (`guid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of items
-- ----------------------------

-- ----------------------------
-- Table structure for `live_action`
-- ----------------------------
DROP TABLE IF EXISTS `live_action`;
CREATE TABLE `live_action` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `PlayerID` int(11) NOT NULL,
  `Action` int(11) NOT NULL,
  `Nombre` int(11) NOT NULL,
  PRIMARY KEY (`ID`)
) ENGINE=MyISAM AUTO_INCREMENT=66 DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of live_action
-- ----------------------------

-- ----------------------------
-- Table structure for `mountpark_data`
-- ----------------------------
DROP TABLE IF EXISTS `mountpark_data`;
CREATE TABLE `mountpark_data` (
  `mapid` int(11) NOT NULL,
  `size` int(11) NOT NULL,
  `owner` int(11) NOT NULL,
  `guild` int(11) NOT NULL DEFAULT '-1',
  `price` int(11) NOT NULL,
  `data` text NOT NULL,
  PRIMARY KEY (`mapid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of mountpark_data
-- ----------------------------

-- ----------------------------
-- Table structure for `mounts_data`
-- ----------------------------
DROP TABLE IF EXISTS `mounts_data`;
CREATE TABLE `mounts_data` (
  `id` int(11) NOT NULL,
  `color` int(11) NOT NULL,
  `sexe` int(11) NOT NULL,
  `name` varchar(30) NOT NULL,
  `xp` int(32) NOT NULL,
  `level` int(11) NOT NULL,
  `endurance` int(11) NOT NULL,
  `amour` int(11) NOT NULL,
  `maturite` int(11) NOT NULL,
  `serenite` int(11) NOT NULL,
  `reproductions` int(11) NOT NULL,
  `fatigue` int(11) NOT NULL,
  `energie` int(11) NOT NULL,
  `items` text NOT NULL,
  `ancetres` varchar(50) NOT NULL DEFAULT ',,,,,,,,,,,,,',
  UNIQUE KEY `id` (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of mounts_data
-- ----------------------------

-- ----------------------------
-- Table structure for `personnages`
-- ----------------------------
DROP TABLE IF EXISTS `personnages`;
CREATE TABLE `personnages` (
  `guid` int(11) NOT NULL,
  `name` varchar(30) NOT NULL,
  `sexe` tinyint(4) NOT NULL,
  `class` smallint(6) NOT NULL,
  `color1` int(11) NOT NULL,
  `color2` int(11) NOT NULL,
  `color3` int(11) NOT NULL,
  `kamas` int(11) NOT NULL,
  `spellboost` int(11) NOT NULL,
  `capital` int(11) NOT NULL,
  `energy` int(11) NOT NULL DEFAULT '10000',
  `level` int(11) NOT NULL,
  `xp` bigint(32) NOT NULL DEFAULT '0',
  `size` int(11) NOT NULL,
  `gfx` int(11) NOT NULL,
  `alignement` int(11) NOT NULL DEFAULT '0',
  `honor` int(11) NOT NULL DEFAULT '0',
  `deshonor` int(11) NOT NULL DEFAULT '0',
  `alvl` int(11) NOT NULL DEFAULT '0' COMMENT 'Niveau alignement',
  `account` int(11) NOT NULL,
  `vitalite` int(11) NOT NULL DEFAULT '0',
  `force` int(11) NOT NULL DEFAULT '0',
  `sagesse` int(11) NOT NULL DEFAULT '0',
  `intelligence` int(11) NOT NULL DEFAULT '0',
  `chance` int(11) NOT NULL DEFAULT '0',
  `agilite` int(11) NOT NULL DEFAULT '0',
  `seeSpell` tinyint(4) NOT NULL DEFAULT '0',
  `seeFriend` tinyint(4) NOT NULL DEFAULT '1',
  `canaux` varchar(15) NOT NULL DEFAULT '*#%!pi$:?',
  `map` int(11) NOT NULL DEFAULT '8479',
  `cell` int(11) NOT NULL,
  `pdvper` int(11) NOT NULL DEFAULT '100',
  `spells` text NOT NULL,
  `objets` text NOT NULL,
  `savepos` varchar(20) NOT NULL DEFAULT '10298,314',
  `zaaps` varchar(250) NOT NULL DEFAULT '',
  `jobs` varchar(300) NOT NULL DEFAULT '',
  `mountxpgive` int(11) NOT NULL DEFAULT '0',
  `mount` int(11) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`guid`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of personnages
-- ----------------------------

-- ----------------------------
-- Table structure for `temp_mapobjects`
-- ----------------------------
DROP TABLE IF EXISTS `temp_mapobjects`;
CREATE TABLE `temp_mapobjects` (
  `mapid` int(11) NOT NULL,
  `cellid` int(11) NOT NULL,
  `tempID` int(11) NOT NULL,
  `data` text NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- ----------------------------
-- Records of temp_mapobjects
-- ----------------------------
