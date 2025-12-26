-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: localhost    Database: exchange
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `candles`
--

DROP TABLE IF EXISTS `candles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `candles` (
  `symbolID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `timeframe` enum('1D','1H','30m','15m','5m','1m') CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `open_time` timestamp NOT NULL,
  `open` decimal(36,18) NOT NULL,
  `high` decimal(36,18) NOT NULL,
  `low` decimal(36,18) NOT NULL,
  `close` decimal(36,18) NOT NULL,
  `close_time` timestamp NOT NULL,
  PRIMARY KEY (`symbolID`,`timeframe`,`open_time`),
  CONSTRAINT `fk_candles_symbols` FOREIGN KEY (`symbolID`) REFERENCES `symbols` (`symbolID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `coins`
--

DROP TABLE IF EXISTS `coins`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coins` (
  `coinID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `decimals` float NOT NULL DEFAULT '0.01',
  PRIMARY KEY (`coinID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `members`
--

DROP TABLE IF EXISTS `members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `members` (
  `memberID` int NOT NULL AUTO_INCREMENT,
  `account` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `password` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(45) CHARACTER SET utf8mb3 NOT NULL,
  `number` varchar(45) CHARACTER SET utf8mb3 DEFAULT '0',
  `join_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`memberID`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `orderID` int NOT NULL AUTO_INCREMENT,
  `memberID` int NOT NULL,
  `symbolID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `side` enum('buy','sell') COLLATE utf8mb3_bin NOT NULL,
  `type` enum('market','limit') COLLATE utf8mb3_bin NOT NULL,
  `price` decimal(36,18) NOT NULL DEFAULT '0.000000000000000000',
  `quantity` decimal(36,18) NOT NULL,
  `filled_quantity` decimal(36,18) NOT NULL DEFAULT '0.000000000000000000',
  `status` enum('new','partial_filled','filled','canceled') CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL DEFAULT 'new',
  `post_only` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`orderID`),
  KEY `member_idx` (`memberID`),
  KEY `fk_orders_symbols` (`symbolID`),
  CONSTRAINT `fk_orders_members` FOREIGN KEY (`memberID`) REFERENCES `members` (`memberID`),
  CONSTRAINT `fk_orders_symbols` FOREIGN KEY (`symbolID`) REFERENCES `symbols` (`symbolID`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `positions`
--

DROP TABLE IF EXISTS `positions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `positions` (
  `positionID` int NOT NULL AUTO_INCREMENT,
  `memberID` int NOT NULL,
  `symbolID` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,
  `side` enum('long','short') CHARACTER SET utf8mb3 NOT NULL,
  `quantity` decimal(36,18) NOT NULL,
  `avgprice` decimal(36,18) NOT NULL,
  `pnl` decimal(36,18) NOT NULL DEFAULT '0.000000000000000000',
  `status` enum('open','closed') CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `open_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `close_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`positionID`),
  KEY `member3_idx` (`memberID`),
  KEY `fk_positions_symbol` (`symbolID`),
  CONSTRAINT `fk_position_member` FOREIGN KEY (`memberID`) REFERENCES `members` (`memberID`),
  CONSTRAINT `fk_positions_symbol` FOREIGN KEY (`symbolID`) REFERENCES `symbols` (`symbolID`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `symbols`
--

DROP TABLE IF EXISTS `symbols`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `symbols` (
  `symbolID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `name` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `base_coinID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `quote_coinID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  PRIMARY KEY (`symbolID`),
  KEY `fk_symbols_coins_quote` (`quote_coinID`),
  KEY `fk_symbols_coins_base` (`base_coinID`),
  CONSTRAINT `fk_symbols_coins_base` FOREIGN KEY (`base_coinID`) REFERENCES `coins` (`coinID`),
  CONSTRAINT `fk_symbols_coins_quote` FOREIGN KEY (`quote_coinID`) REFERENCES `coins` (`coinID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `trades`
--

DROP TABLE IF EXISTS `trades`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trades` (
  `tradesID` int NOT NULL AUTO_INCREMENT,
  `symbolID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `taker_orderID` int NOT NULL,
  `maker_orderID` int NOT NULL,
  `price` decimal(36,18) NOT NULL,
  `quantity` decimal(36,18) NOT NULL,
  `taker_side` enum('buy','sell') COLLATE utf8mb3_bin NOT NULL,
  `executed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `fee_currency` varchar(45) COLLATE utf8mb3_bin DEFAULT NULL,
  `fee_amount` decimal(36,18) DEFAULT NULL,
  PRIMARY KEY (`tradesID`),
  KEY `fk_trades_symbols` (`symbolID`) /*!80000 INVISIBLE */,
  KEY `taker_order` (`taker_orderID`) /*!80000 INVISIBLE */,
  KEY `order1_idx` (`maker_orderID`),
  KEY `fk_trades_coins` (`fee_currency`),
  CONSTRAINT `fk_trades_coins` FOREIGN KEY (`fee_currency`) REFERENCES `coins` (`coinID`),
  CONSTRAINT `fk_trades_order_maker` FOREIGN KEY (`maker_orderID`) REFERENCES `orders` (`orderID`),
  CONSTRAINT `fk_trades_order_taker` FOREIGN KEY (`taker_orderID`) REFERENCES `orders` (`orderID`),
  CONSTRAINT `fk_trades_symbols` FOREIGN KEY (`symbolID`) REFERENCES `symbols` (`symbolID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `wallets`
--

DROP TABLE IF EXISTS `wallets`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wallets` (
  `memberID` int NOT NULL,
  `coinID` varchar(45) COLLATE utf8mb3_bin NOT NULL,
  `balance` decimal(36,18) NOT NULL DEFAULT '0.000000000000000000',
  `available` decimal(36,18) NOT NULL DEFAULT '0.000000000000000000',
  PRIMARY KEY (`memberID`,`coinID`),
  KEY `memberID_idx` (`memberID`),
  KEY `fk_wallets_coins` (`coinID`),
  CONSTRAINT `fk_wallets_coins` FOREIGN KEY (`coinID`) REFERENCES `coins` (`coinID`),
  CONSTRAINT `fk_wallets_members` FOREIGN KEY (`memberID`) REFERENCES `members` (`memberID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-18 21:52:26
