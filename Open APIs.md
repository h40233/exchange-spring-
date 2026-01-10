# 📡 開放 API 列表 (Open APIs List)

本文件整理了星際交易所 (Interstellar Exchange) 後端開放的所有 RESTful API 接口。

## 1. 會員系統 (Member System)
負責處理使用者的註冊、登入與個人資料管理。
* **Controller**: `MemberController`

| HTTP 方法 | 路徑 (Endpoint) | 功能描述 | 需登入 (Session) | 備註 |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/members/register` | **註冊**：建立新會員帳號 | ❌ 否 | 需提供帳號、密碼、姓名、電話 |
| `POST` | `/api/members/login` | **登入**：驗證帳密並建立 Session | ❌ 否 | 登入成功後 Server 會回傳 Cookie |
| `POST` | `/api/members/logout` | **登出**：銷毀當前 Session | ✅ 是 | |
| `GET` | `/api/members/me` | **查詢個資**：獲取當前登入者資訊 | ✅ 是 | |
| `PUT` | `/api/members/me` | **更新個資**：修改姓名、電話或密碼 | ✅ 是 | 僅更新有傳送的欄位 |

---

## 2. 錢包系統 (Wallet System)
負責資產查詢、資金流水與測試用的資金操作。
* **Controller**: `WalletController`

| HTTP 方法 | 路徑 (Endpoint) | 功能描述 | 需登入 (Session) | 備註 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/wallets` | **我的錢包**：列出所有幣種餘額 | ✅ 是 | 包含總額與可用餘額 |
| `GET` | `/api/wallets/coins` | **幣種列表**：獲取系統支援幣種 | ❌ 否 | 用於前端下拉選單 (如 BTC, USDT) |
| `POST` | `/api/wallets/deposit` | **模擬儲值**：增加特定幣種餘額 | ✅ 是 | 測試用功能，可直接入金 |
| `POST` | `/api/wallets/reset` | **資產重置**：將所有餘額歸零 | ✅ 是 | 測試用功能，清空所有資產 |
| `GET` | `/api/wallets/transactions`| **資金流水**：查詢充值、交易扣款紀錄 | ✅ 是 | 依時間倒序排列 |

---

## 3. 訂單系統 (Order System)
負責委託單的管理、下單、撤單與查詢成交紀錄。
* **Controller**: `OrderController`

| HTTP 方法 | 路徑 (Endpoint) | 功能描述 | 需登入 (Session) | 備註 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/orders` | **歷史委託**：查詢我的訂單列表 | ✅ 是 | 包含未成交與已成交訂單 |
| `POST` | `/api/orders` | **下單**：建立買單或賣單 | ✅ 是 | 支援 `LIMIT` (限價) 與 `MARKET` (市價) |
| `POST` | `/api/orders/{id}/cancel` | **撤單**：取消未成交的訂單 | ✅ 是 | 僅限狀態為 `NEW` 或 `PARTIAL_FILLED` |
| `GET` | `/api/orders/trades` | **成交紀錄**：查詢撮合成功的詳細紀錄 | ✅ 是 | 包含 Taker 與 Maker 視角 |
| `GET` | `/api/orders/book/{symbol}`| **訂單簿**：查詢買賣盤深度 | ❌ 否 | 例如查詢 BTCUSDT 的深度 |

---

## 4. 市場行情 (Market Data)
負責提供公開的市場資訊，如價格、K 線圖。
* **Controller**: `SymbolController`, `CandleController`

| HTTP 方法 | 路徑 (Endpoint) | 功能描述 | 需登入 (Session) | 備註 |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/symbols/coins` | **可交易幣種**：獲取用於建立交易對的清單 | ❌ 否 | 同 `/api/wallets/coins` |
| `GET` | `/api/symbols/tickers` | **最新報價**：獲取所有交易對的最新成交價 | ❌ 否 | 回傳 Map 格式，如 `{"BTC": 50000}` |
| `GET` | `/api/candles/{symbol}` | **K線數據**：獲取 OHLCV 歷史數據 | ❌ 否 | 支援 `interval` 參數 (如 1m, 1h) |

> **注意**：`PositionController` (合約倉位) 相關接口目前尚未啟用。