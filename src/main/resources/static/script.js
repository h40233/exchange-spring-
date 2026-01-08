// ====== 全域常數與設定 ======
const API_URL = '/api/members';
const WALLET_API_URL = '/api/wallets';
let SUPPORTED_COINS = []; // 將從後端動態獲取

// ====== 全域狀態 (State Management) ======
// 在 SPA 中，狀態管理非常重要。這裡使用簡單的變數來儲存狀態。
let orderBookInterval = null; // 訂單簿輪詢計時器 ID
let currentTradeType = 'SPOT'; // 當前交易模式: 'SPOT' (現貨) 或 'CONTRACT' (合約)
let currentOrderSide = 'BUY'; // 當前下單方向: 'BUY' 或 'SELL'
let currentHistoryTab = 'FUNDS'; // 當前歷史紀錄分頁

// ====== 初始化 (Initialization) ======
window.onload = async () => {
    setupDropdown(); // 初始化下拉選單事件
    await fetchSupportedCoins(); // 獲取系統支援幣種
    // 檢查使用者是否已登入 (檢查 Session)
    const res = await fetch(`${API_URL}/me`);
    if(res.ok) {
        showDashboard(); // 已登入 -> 顯示儀表板
    } else {
        showLogin(); // 未登入 -> 顯示登入頁
    }
};

// 從後端獲取支援的幣種列表
async function fetchSupportedCoins() {
    try {
        const res = await fetch('/api/symbols/coins');
        if (res.ok) {
            SUPPORTED_COINS = await res.json();
        } else {
            console.error('Failed to fetch supported coins');
            SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB']; // 備援預設值
        }
    } catch (err) {
        console.error(err);
        SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB'];
    }
}

// ====== 導航系統 (Navigation System) ======

// 隱藏所有面板，並清理狀態 (如計時器)
function hideAllPanels() {
    const panels = ['loginPanel', 'registerPanel', 'dashboardPanel', 'profilePanel', 'walletPanel', 'tradePanel'];
    panels.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden'); // 使用 CSS class 控制顯示
    });
    closeDepositModal();
    clearMsgs();

    // 離開交易頁面時，停止訂單簿輪詢，節省資源
    if (orderBookInterval) {
        clearInterval(orderBookInterval);
        orderBookInterval = null;
    }
}

// 顯示特定面板的通用函式
function showPanel(panelId) {
    hideAllPanels();
    document.getElementById(panelId).classList.remove('hidden');
}

// 各功能頁面的進入點
function showLogin() { showPanel('loginPanel'); }
function showRegister() { showPanel('registerPanel'); }

function showDashboard() {
    showPanel('dashboardPanel');
    fetchSimpleProfile(); // 更新歡迎訊息中的名字
}

// --- 交易模式切換邏輯 ---

function showSpot() {
    showPanel('tradePanel');
    
    // 設定 UI 標題
    document.getElementById('tradePanelTitle').innerText = '交易中心 (Trading Center)';
    // 隱藏合約專用的區塊
    if(document.getElementById('positionSection')) document.getElementById('positionSection').style.display = 'none'; 
    if(document.getElementById('tabHistoryPnL')) document.getElementById('tabHistoryPnL').style.display = 'none'; 

    // 設定內部狀態
    setTradeType('SPOT'); 

    // 預設顯示資金流水
    switchHistoryTab('FUNDS'); 
}

// ====== 歷史紀錄模組 (History Module) ======

// 切換歷史紀錄分頁 (資金 / 成交 / 盈虧)
function switchHistoryTab(tab) {
    currentHistoryTab = tab;
    
    // 更新按鈕樣式 (Active State)
    const buttons = {
        'FUNDS': 'tabHistoryFunds',
        'TRADES': 'tabHistoryTrades',
        'PNL': 'tabHistoryPnL'
    };
    
    Object.keys(buttons).forEach(key => {
        const btnId = buttons[key];
        const btn = document.getElementById(btnId);
        if (!btn) return;

        if (key === tab) {
            btn.className = 'btn btn-sm';
            btn.style.background = 'var(--neon-blue)';
            btn.style.color = 'black';
        } else {
            btn.className = 'btn btn-sm btn-secondary';
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
        }
    });

    // 顯示載入中狀態
    const content = document.getElementById('historyContent');
    content.innerHTML = '<div style="text-align:center; padding:20px; color:#aaa;">載入中...</div>';

    // 根據 Tab 呼叫對應 API
    if (tab === 'FUNDS') {
        fetchHistoryFunds();
    } else if (tab === 'TRADES') {
        fetchHistoryTrades(currentTradeType);
    } else if (tab === 'PNL') {
        fetchHistoryPnL();
    }
}

// 獲取資金流水
async function fetchHistoryFunds() {
    try {
        console.log("Fetching Funds History...");
        const res = await fetch('/api/wallets/transactions');
        if (res.ok) {
            const data = await res.json();
            renderHistoryFunds(data); // 呼叫渲染函式
        } else {
            document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">無法載入資料</div>';
        }
    } catch (err) {
        console.error(err);
        document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">連線錯誤</div>';
    }
}

// 渲染資金流水 HTML
// [註1] 渲染優化：使用 Template Literals 構建 HTML 字串再一次性寫入 innerHTML，比頻繁操作 DOM 更快。
function renderHistoryFunds(data) {
    const el = document.getElementById('historyContent');
    if (!data || data.length === 0) {
        el.innerHTML = '<div style="text-align:center; padding:20px; color:#777;">暫無資金紀錄</div>';
        return;
    }

    let html = `
        <table style="width:100%; border-collapse:collapse; font-size:0.9em;">
            <thead>
                <tr style="border-bottom:1px solid #555; color:#aaa;">
                    <th style="padding:10px; text-align:left;">時間</th>
                    <th style="padding:10px; text-align:left;">類型</th>
                    <th style="padding:10px; text-align:left;">幣種</th>
                    <th style="padding:10px; text-align:right;">金額</th>
                </tr>
            </thead>
            <tbody>
    `;

    data.forEach(item => {
        const date = new Date(item.createdAt).toLocaleString();
        const color = item.amount >= 0 ? '#00ff88' : '#ff4d4d'; // 正數綠色，負數紅色
        const typeLabel = item.type || 'UNKNOWN';
        
        html += `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:10px;">${date}</td>
                <td style="padding:10px;">${typeLabel}</td>
                <td style="padding:10px;">${item.coinId}</td>
                <td style="padding:10px; text-align:right; color:${color};">${item.amount}</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    el.innerHTML = html;
}

// ... (fetchHistoryTrades 與 fetchHistoryPnL 邏輯類似，省略註解) ...

// ====== 交易核心邏輯 (Trading Core) ======

let symbolOptions = [];

// 設定交易模式與下拉選單內容
function setTradeType(type) {
    currentTradeType = type;
    
    const label = document.getElementById('labelSymbol');
    const hiddenInput = document.getElementById('tradeSymbol'); 
    
    const currentVal = hiddenInput.value;
    symbolOptions = [];
    const coins = SUPPORTED_COINS.filter(c => c !== 'USDT');

    // 根據模式產生選單選項
    if (type === 'SPOT') {
        if(label) label.innerText = '幣種 (Coin)';
        coins.forEach(coin => {
            symbolOptions.push({
                value: coin + 'USDT', // e.g. BTCUSDT
                text: coin
            });
        });
    } else {
        if(label) label.innerText = '交易對 (Symbol)';
        coins.forEach(coin => {
            symbolOptions.push({
                value: coin + 'USDT',
                text: coin + '/USDT'
            });
        });
    }

    // 預設選取第一個，或保留當前選取
    const found = symbolOptions.find(o => o.value === currentVal);
    if (found) {
        selectSymbol(found.value, found.text, false); 
    } else if (symbolOptions.length > 0) {
        selectSymbol(symbolOptions[0].value, symbolOptions[0].text, false);
    }
    
    // 初始化數據載入
    fetchMyOrders();
    fetchOrderBook();
    
    // 啟動輪詢 (Polling) 機制
    // [註2] 即時性：這裡使用 setInterval 每秒拉取一次訂單簿。
    // 在生產環境中，建議改用 WebSocket 以降低伺服器負載並提升即時性。
    console.log("Starting Order Book Polling for " + type);
    if (orderBookInterval) clearInterval(orderBookInterval);
    orderBookInterval = setInterval(fetchOrderBook, 1000);
}

// ... (setupDropdown, renderSymbolDropdown 邏輯省略) ...

// 下單函式
async function submitOrder() {
    const symbolId = document.getElementById('tradeSymbol').value;
    const type = document.getElementById('tradeType').value;
    const price = document.getElementById('tradePrice').value;
    const quantity = document.getElementById('tradeQuantity').value;

    // 基礎前端驗證
    if (!price || price <= 0 || !quantity || quantity <= 0) {
        alert('請輸入有效的價格與數量');
        return;
    }

    const payload = {
        symbolId: symbolId,
        side: currentOrderSide,
        tradeType: currentTradeType,
        type: type,
        price: parseFloat(price),
        quantity: parseFloat(quantity)
    };

    try {
        const res = await fetch('/api/orders', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const data = await res.json();
            alert(`下單成功！單號: ${data.orderId}`);
            // 清空輸入框
            document.getElementById('tradePrice').value = '';
            document.getElementById('tradeQuantity').value = '';
            // 立即刷新數據
            fetchMyOrders(); 
            fetchOrderBook(); 
            if (currentTradeType === 'CONTRACT') fetchPositions();
        } else {
            const txt = await res.text();
            alert('下單失敗: ' + txt);
        }
    } catch (err) {
        console.error(err);
        alert('連線錯誤');
    }
}

// ====== 訂單管理 (Order Management) ======

// 本地端過濾訂單列表 (Client-side Filtering)
function renderOrders() {
    const listEl = document.getElementById('orderList');
    if (!listEl) return;
    listEl.innerHTML = '';

    // 根據當前篩選器 (Filter) 過濾訂單
    const orders = allMyOrders.filter(o => {
        const oType = (o.tradeType || 'CONTRACT'); 
        if (oType !== currentTradeType) return false; // 只顯示當前模式的訂單

        if (currentOrderFilter === 'ALL') return true;
        if (currentOrderFilter === 'OPEN') return (o.status === 'NEW' || o.status === 'PARTIAL_FILLED');
        if (currentOrderFilter === 'FILLED') return (o.status === 'FILLED' || o.status === 'PARTIAL_FILLED');
        if (currentOrderFilter === 'CANCELED') return (o.status === 'CANCELED');
        return true;
    });

    // ... (HTML 生成邏輯省略) ...
}

// ====== 訂單簿 (Order Book) ======
async function fetchOrderBook() {
    const symbolId = document.getElementById('tradeSymbol').value;
    if(!symbolId) return;
    try {
        // 從 API 獲取深度資料
        const res = await fetch(`/api/orders/book/${symbolId}?type=${currentTradeType}`);
        if (res.ok) {
            const data = await res.json();
            renderOrderBook(data);
        }
    } catch (err) {
        console.error(err);
    }
}

// 渲染訂單簿
function renderOrderBook(data) {
    const asksEl = document.getElementById('orderBookAsks');
    const bidsEl = document.getElementById('orderBookBids');
    if (!asksEl || !bidsEl) return;
    
    asksEl.innerHTML = '';
    bidsEl.innerHTML = '';

    // 渲染賣盤 (Asks)
    data.asks.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#ff4d4d', 'SELL');
        asksEl.appendChild(div);
    });

    // 渲染買盤 (Bids)
    data.bids.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#00f3ff', 'BUY');
        bidsEl.appendChild(div);
    });
}

// 建立訂單簿的單行 DOM
function createBookItem(price, qty, color, itemSide) {
    const div = document.createElement('div');
    // ... (樣式設定) ...
    
    // 點擊價格自動填入下單區
    div.onclick = () => {
        document.getElementById('tradePrice').value = price;
        // 如果點擊買單，則自動切換為賣出 (因為你想賣給他)，反之亦然
        const targetSide = (itemSide === 'BUY') ? 'SELL' : 'BUY';
        setOrderSide(targetSide);
    };
    
    // ... (HTML 生成) ...
    return div;
}

// ... (Wallet 相關邏輯省略) ...

// ====== 備註區 ======
/*
[註1] 安全性 (XSS):
      `renderHistoryFunds` 等函式使用 `innerHTML` 拼接 HTML 字串。
      若後端回傳的 `item.type` 或 `item.coinId` 包含惡意腳本 (例如 `<script>alert(1)</script>`)，
      瀏覽器可能會執行它。
      改進建議：使用 `document.createElement` 與 `textContent` 來構建 DOM，或使用 DOMPurify 函式庫過濾 HTML。

[註2] 效能 (Polling vs WebSocket):
      目前使用 `setInterval` 每秒輪詢訂單簿。這會對伺服器造成大量 HTTP 請求壓力。
      對於即時交易系統，強烈建議改用 WebSocket (STOMP over WebSocket) 來推送數據。

[註3] 模組化 (Modularization):
      `script.js` 檔案過大，包含了 API、UI、邏輯混合在一起。
      改進建議：拆分為 `api.js`, `ui.js`, `auth.js` 等模組，並使用 ES6 Modules (`import/export`) 管理。
*/