CREATE TABLE IF NOT EXISTS `hdv_ventes` (
  `guid` int(11) NOT NULL,
  `seller` int(11) NOT NULL,
  `quantity` int(11) NOT NULL,
  `templateid` int(11) NOT NULL,
  `stats` text NOT NULL,
  `item_type` int(11) NOT NULL,
  `KamasSell` int(20) NOT NULL,
  `TimeSell` int(11) NOT NULL,
  `hdv_type` int(11) NOT NULL,
  PRIMARY KEY (`guid`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1;

--
-- Contenu de la table `hdv_ventes`
--