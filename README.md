# 💱 交易所資料庫架構（Exchange Database Schema）

---

## 🧑‍💻 `members`
> 紀錄會員資料以及登入憑證

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `memberID` | INT | 會員 ID（主鍵, Auto Increment） |
| `account` | VARCHAR(45) | 登入帳號 |
| `password` | VARCHAR(45) | 登入密碼 |
| `name` | VARCHAR(45) | 姓名 |
| `number` | VARCHAR(45) | 電話 |
| `join_time` | TIMESTAMP | 加入時間（預設當下時間） |

---

## 🪙 `coins`
> 紀錄交易所目前可交易的幣種資訊

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `coinID` | VARCHAR(45) | 幣種 ID（幣種簡稱，如 BTC, USDT） |
| `name` | VARCHAR(45) | 幣種中文全稱 |
| `decimals` | FLOAT | 該幣種的價格精度（預設 0.01） |

---

## 💰 `wallets`
> 紀錄會員的帳戶餘額資訊

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `memberID` | INT | 哪一個會員的錢包（FK → `members.memberID`） |
| `coinID` | VARCHAR(45) | 哪一個幣種的餘額（FK → `coins.coinID`） |
| `balance` | DECIMAL(36,18) | 該幣種的總餘額 |
| `available` | DECIMAL(36,18) | 該幣種的可用餘額 |

> 🔑 **主鍵**：(`memberID`, `coinID`) — 每位會員每個幣種僅有一筆錢包紀錄。

---

## 🔁 `symbols`
> 紀錄交易所目前可交易的幣對資訊

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `symbolID` | VARCHAR(45) | 幣對 ID（簡稱，如 BTCUSDT） |
| `name` | VARCHAR(45) | 幣對名稱 |
| `base_coinID` | VARCHAR(45) | 基幣（FK → `coins.coinID`） |
| `quote_coinID` | VARCHAR(45) | 報價幣（FK → `coins.coinID`） |

---

## 📊 `candles`
> 紀錄 K 線資料，用於模擬交易與圖表繪製

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `symbolID` | VARCHAR(45) | 幣對 ID（FK → `symbols.symbolID`） |
| `timeframe` | ENUM | K 線時間框架 ('1D','1H','30m','15m','5m','1m') |
| `open_time` | TIMESTAMP | K 線開始時間 |
| `open` | DECIMAL(36,18) | 開盤價 |
| `high` | DECIMAL(36,18) | 最高價 |
| `low` | DECIMAL(36,18) | 最低價 |
| `close` | DECIMAL(36,18) | 收盤價 |
| `close_time` | TIMESTAMP | K 線結束時間 |

> 🔑 **主鍵**：(`symbolID`, `timeframe`, `open_time`)

---

## 📑 `orders`
> 紀錄委託單資訊

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `orderID` | INT | 訂單 ID（主鍵, Auto Increment） |
| `memberID` | INT | 會員 ID（FK → `members.memberID`） |
| `symbolID` | VARCHAR(45) | 幣對 ID（FK → `symbols.symbolID`） |
| `side` | ENUM | 訂單方向 ('buy','sell') |
| `type` | ENUM | 訂單類型 ('market','limit') |
| `price` | DECIMAL(36,18) | 下單價格 |
| `quantity` | DECIMAL(36,18) | 下單數量 |
| `filled_quantity` | DECIMAL(36,18) | 已成交數量 |
| `status` | ENUM | 訂單狀態 ('new','partial_filled','filled','canceled') |
| `post_only` | TINYINT(1) | 是否只做 maker（0=否 / 1=是） |
| `created_at` | TIMESTAMP | 掛單時間 |
| `updated_at` | TIMESTAMP | 狀態更新時間 |

---

## 🔄 `trades`
> 紀錄撮合歷史（成交紀錄）

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `tradesID` | INT | 成交紀錄 ID（主鍵, Auto Increment） |
| `symbolID` | VARCHAR(45) | 幣對 ID（FK → `symbols.symbolID`） |
| `taker_orderID` | INT | taker 的訂單 ID（FK → `orders.orderID`） |
| `maker_orderID` | INT | maker 的訂單 ID（FK → `orders.orderID`） |
| `price` | DECIMAL(36,18) | 成交價格 |
| `quantity` | DECIMAL(36,18) | 成交量 |
| `taker_side` | ENUM | taker 的方向 ('buy','sell') |
| `executed_at` | TIMESTAMP | 成交時間 |
| `fee_currency` | VARCHAR(45) | 手續費使用的幣別（FK → `coins.coinID`） |
| `fee_amount` | DECIMAL(36,18) | 手續費金額 |

---

## 📈 `positions`
> 紀錄會員倉位（開倉 / 平倉歷史）

| 欄位名稱 | 型態 | 說明 |
|-----------|--------|------|
| `positionID` | INT | 倉位 ID（主鍵, Auto Increment） |
| `memberID` | INT | 會員 ID（FK → `members.memberID`） |
| `symbolID` | VARCHAR(45) | 幣對 ID（FK → `symbols.symbolID`） |
| `side` | ENUM | 倉位方向 ('long','short') |
| `quantity` | DECIMAL(36,18) | 倉位數量 |
| `avgprice` | DECIMAL(36,18) | 平均開倉價格 |
| `pnl` | DECIMAL(36,18) | 倉位盈虧 |
| `status` | ENUM | 倉位狀態 ('open','closed') |
| `open_at` | TIMESTAMP | 開倉時間 |
| `close_at` | TIMESTAMP | 平倉時間 |

---

## 🔗 關聯關係總覽 (Relationships)
```mermaid
erDiagram
    members ||--o{ wallets : "has"
    members ||--o{ orders : "places"
    members ||--o{ positions : "holds"
    
    coins ||--o{ wallets : "currency"
    coins ||--o{ symbols : "base/quote"
    coins ||--o{ trades : "fee"

    symbols ||--o{ candles : "history"
    symbols ||--o{ orders : "target"
    symbols ||--o{ trades : "target"
    symbols ||--o{ positions : "target"

    orders ||--o{ trades : "executes"