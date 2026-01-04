const API_URL = '/api/members';
const WALLET_API_URL = '/api/wallets';
let SUPPORTED_COINS = []; 

// --- Global State ---
let orderBookInterval = null;
let currentTradeType = 'SPOT'; // SPOT or CONTRACT
let currentOrderSide = 'BUY'; // BUY or SELL
let currentHistoryTab = 'FUNDS';

// --- Initialization ---
window.onload = async () => {
    setupDropdown();
    await fetchSupportedCoins();
    const res = await fetch(`${API_URL}/me`);
    if(res.ok) {
        showDashboard();
    } else {
        showLogin();
    }
};

async function fetchSupportedCoins() {
    try {
        const res = await fetch('/api/symbols/coins');
        if (res.ok) {
            SUPPORTED_COINS = await res.json();
        } else {
            console.error('Failed to fetch supported coins');
            SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB'];
        }
    } catch (err) {
        console.error(err);
        SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB'];
    }
}

// --- Navigation ---

function hideAllPanels() {
    const panels = ['loginPanel', 'registerPanel', 'dashboardPanel', 'profilePanel', 'walletPanel', 'tradePanel'];
    panels.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });
    closeDepositModal();
    clearMsgs();

    if (orderBookInterval) {
        clearInterval(orderBookInterval);
        orderBookInterval = null;
    }
}

function showPanel(panelId) {
    hideAllPanels();
    document.getElementById(panelId).classList.remove('hidden');
}

function showLogin() { showPanel('loginPanel'); }
function showRegister() { showPanel('registerPanel'); }

function showDashboard() {
    showPanel('dashboardPanel');
    fetchSimpleProfile();
}

function showProfile() {
    showPanel('profilePanel');
    disableEditMode();
    fetchProfileDetail(); 
}

function showWallet() {
    showPanel('walletPanel');
    fetchWallets();
}

// --- New Navigation Logic for Spot / Contract ---

function showSpot() {
    showPanel('tradePanel');
    
    // UI Adjustments for Spot
    document.getElementById('tradePanelTitle').innerText = '交易中心 (Trading Center)';
    if(document.getElementById('positionSection')) document.getElementById('positionSection').style.display = 'none'; 
    if(document.getElementById('tabHistoryPnL')) document.getElementById('tabHistoryPnL').style.display = 'none'; 

    // Logic Setup
    setTradeType('SPOT'); 

    // History Setup
    switchHistoryTab('FUNDS'); 
}

/*
function showContract() {
    showPanel('tradePanel');

    // UI Adjustments for Contract
    document.getElementById('tradePanelTitle').innerText = '合約交易中心 (Contract Trading)';
    document.getElementById('positionSection').style.display = 'block'; // Show positions
    document.getElementById('tabHistoryPnL').style.display = 'inline-block'; // Show PnL tab

    // Logic Setup
    setTradeType('CONTRACT');

    // History Setup
    switchHistoryTab('FUNDS');
}
*/


// --- History Logic ---

function switchHistoryTab(tab) {
    currentHistoryTab = tab;
    
    // Update Buttons
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

    const content = document.getElementById('historyContent');
    content.innerHTML = '<div style="text-align:center; padding:20px; color:#aaa;">載入中...</div>';

    if (tab === 'FUNDS') {
        fetchHistoryFunds();
    } else if (tab === 'TRADES') {
        // Fetch trades based on current Trade Type (Spot or Contract)
        fetchHistoryTrades(currentTradeType);
    } else if (tab === 'PNL') {
        fetchHistoryPnL();
    }
}

async function fetchHistoryFunds() {
    try {
        console.log("Fetching Funds History...");
        const res = await fetch('/api/wallets/transactions');
        if (res.ok) {
            const data = await res.json();
            console.log("Funds History Data:", data);
            renderHistoryFunds(data);
        } else {
            document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">無法載入資料</div>';
        }
    } catch (err) {
        console.error(err);
        document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">連線錯誤</div>';
    }
}

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
        const color = item.amount >= 0 ? '#00ff88' : '#ff4d4d';
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

async function fetchHistoryTrades(filterType) {
    try {
        console.log(`Fetching Trades History for ${filterType}...`);
        const res = await fetch('/api/orders/trades');
        if (res.ok) {
            const data = await res.json();
            console.log("All Trades Data:", data);
            
            // Client-side filtering with Case Insensitivity
            const filtered = data.filter(t => {
                const type = (t.tradeType || 'CONTRACT').toUpperCase(); 
                return type === filterType;
            });
            renderHistoryTrades(filtered, filterType);
        } else {
             document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">無法載入資料</div>';
        }
    } catch (err) { console.error(err); }
}

function renderHistoryTrades(data, type) {
    const el = document.getElementById('historyContent');
    if (!data || data.length === 0) {
        el.innerHTML = `<div style="text-align:center; padding:20px; color:#777;">暫無${type === 'SPOT' ? '現貨' : '合約'}成交紀錄</div>`;
        return;
    }

    let html = `
        <table style="width:100%; border-collapse:collapse; font-size:0.9em;">
            <thead>
                <tr style="border-bottom:1px solid #555; color:#aaa;">
                    <th style="padding:10px; text-align:left;">時間</th>
                    <th style="padding:10px; text-align:left;">交易對</th>
                    <th style="padding:10px; text-align:left;">方向</th>
                    <th style="padding:10px; text-align:right;">價格</th>
                    <th style="padding:10px; text-align:right;">數量</th>
                </tr>
            </thead>
            <tbody>
    `;

    data.forEach(item => {
        const date = new Date(item.executedAt).toLocaleString();
        const side = item.side || 'BUY'; 
        const color = side === 'BUY' ? '#00f3ff' : '#ff4d4d';
        const role = item.role ? `<span style="font-size:0.8em; color:#aaa; margin-left:5px;">(${item.role})</span>` : '';

        html += `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:10px;">${date}</td>
                <td style="padding:10px;">${item.symbolId}</td>
                <td style="padding:10px; color:${color};">${side} ${role}</td>
                <td style="padding:10px; text-align:right;">${item.price}</td>
                <td style="padding:10px; text-align:right;">${item.quantity}</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    el.innerHTML = html;
}

async function fetchHistoryPnL() {
    try {
        const res = await fetch('/api/positions/history');
        if (res.ok) {
            const data = await res.json();
            renderHistoryPnL(data);
        } else {
             document.getElementById('historyContent').innerHTML = '<div style="text-align:center; color:#ff4d4d;">無法載入資料</div>';
        }
    } catch (err) { console.error(err); }
}

function renderHistoryPnL(data) {
    const el = document.getElementById('historyContent');
    const closed = data.filter(p => p.status === 'CLOSED');

    if (closed.length === 0) {
        el.innerHTML = '<div style="text-align:center; padding:20px; color:#777;">暫無平倉/盈虧紀錄</div>';
        return;
    }

    let html = `
        <table style="width:100%; border-collapse:collapse; font-size:0.9em;">
            <thead>
                <tr style="border-bottom:1px solid #555; color:#aaa;">
                    <th style="padding:10px; text-align:left;">平倉時間</th>
                    <th style="padding:10px; text-align:left;">交易對</th>
                    <th style="padding:10px; text-align:left;">方向</th>
                    <th style="padding:10px; text-align:right;">均價</th>
                    <th style="padding:10px; text-align:right;">盈虧 (PnL)</th>
                </tr>
            </thead>
            <tbody>
    `;

    closed.forEach(item => {
        const date = item.closeAt ? new Date(item.closeAt).toLocaleString() : '-';
        const pnl = item.pnl || 0;
        const color = pnl >= 0 ? '#00ff88' : '#ff4d4d';
        const pnlStr = pnl > 0 ? `+${pnl}` : `${pnl}`;

        html += `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:10px;">${date}</td>
                <td style="padding:10px;">${item.symbolId}</td>
                <td style="padding:10px;">${item.side}</td>
                <td style="padding:10px; text-align:right;">${item.avgprice}</td>
                <td style="padding:10px; text-align:right; color:${color}; font-weight:bold;">${pnlStr}</td>
            </tr>
        `;
    });

    html += '</tbody></table>';
    el.innerHTML = html;
}


// --- Trade Logic (Dropdowns, OrderBook, Submit) ---

let symbolOptions = [];

function setTradeType(type) {
    currentTradeType = type;
    
    // We do NOT toggle sections here anymore, as showSpot/showContract handles it.
    // We only setup data-driven things.

    const label = document.getElementById('labelSymbol');
    const hiddenInput = document.getElementById('tradeSymbol'); 
    
    const currentVal = hiddenInput.value;
    symbolOptions = [];
    const coins = SUPPORTED_COINS.filter(c => c !== 'USDT');

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

    // Default selection logic
    const found = symbolOptions.find(o => o.value === currentVal);
    if (found) {
        selectSymbol(found.value, found.text, false); 
    } else if (symbolOptions.length > 0) {
        selectSymbol(symbolOptions[0].value, symbolOptions[0].text, false);
    }
    
    fetchMyOrders();
    fetchOrderBook();
    // if (type === 'CONTRACT') fetchPositions();
    
    // Interval Management
    console.log("Starting Order Book Polling for " + type);
    if (orderBookInterval) clearInterval(orderBookInterval);
    orderBookInterval = setInterval(fetchOrderBook, 1000);
}

function setupDropdown() {
    const searchInput = document.getElementById('tradeSymbolSearch');
    const listEl = document.getElementById('tradeSymbolList');

    if (!searchInput || !listEl) return;

    searchInput.addEventListener('focus', () => {
        renderSymbolDropdown(searchInput.value);
        listEl.classList.remove('hidden');
    });

    searchInput.addEventListener('input', () => {
        renderSymbolDropdown(searchInput.value);
        listEl.classList.remove('hidden');
    });

    // Close when clicking outside
    document.addEventListener('click', (e) => {
        const isClickInside = searchInput.contains(e.target) || listEl.contains(e.target);
        if (!isClickInside) {
            listEl.classList.add('hidden');
        }
    });
}

function renderSymbolDropdown(filterText) {
    const listEl = document.getElementById('tradeSymbolList');
    listEl.innerHTML = '';
    
    const filterUpper = (filterText || '').toUpperCase();
    
    const filtered = symbolOptions.filter(o => {
         return o.text.toUpperCase().includes(filterUpper) || o.value.toUpperCase().includes(filterUpper);
    });

    if (filtered.length === 0) {
        const div = document.createElement('div');
        div.className = 'dropdown-item';
        div.innerText = '無搜尋結果';
        div.style.color = '#777';
        div.style.cursor = 'default';
        listEl.appendChild(div);
        return;
    }

    filtered.forEach(opt => {
        const div = document.createElement('div');
        div.className = 'dropdown-item';
        div.innerText = opt.text;
        div.onclick = () => {
            selectSymbol(opt.value, opt.text, true);
            listEl.classList.add('hidden');
        };
        listEl.appendChild(div);
    });
}

function selectSymbol(value, text, shouldFetch) {
    document.getElementById('tradeSymbol').value = value; 
    document.getElementById('tradeSymbolSearch').value = text;
    if (shouldFetch) {
        fetchOrderBook();
        fetchOrderBook(); // Double fetch or just consistency?
    }
}

function setOrderSide(side) {
    currentOrderSide = side;
    const btnBuy = document.getElementById('btnBuy');
    const btnSell = document.getElementById('btnSell');

    if (side === 'BUY') {
        btnBuy.className = 'btn';
        btnBuy.style.background = 'var(--neon-blue)';
        btnBuy.style.color = 'black';
        
        btnSell.className = 'btn btn-secondary';
        btnSell.style.background = 'transparent';
        btnSell.style.color = 'var(--star-white)';
    } else {
        btnSell.className = 'btn';
        btnSell.style.background = '#ff4d4d'; // Red for sell
        btnSell.style.color = 'white';

        btnBuy.className = 'btn btn-secondary';
        btnBuy.style.background = 'transparent';
        btnBuy.style.color = 'var(--star-white)';
    }
}

async function submitOrder() {
    const symbolId = document.getElementById('tradeSymbol').value;
    const type = document.getElementById('tradeType').value;
    const price = document.getElementById('tradePrice').value;
    const quantity = document.getElementById('tradeQuantity').value;

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
            document.getElementById('tradePrice').value = '';
            document.getElementById('tradeQuantity').value = '';
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

// --- Order Filtering ---
let allMyOrders = [];
let currentOrderFilter = 'ALL';

async function fetchMyOrders() {
    try {
        const res = await fetch('/api/orders');
        if (res.ok) {
            allMyOrders = await res.json();
            renderOrders();
        }
    } catch (err) { console.error(err); }
}

function setOrderFilter(filter) {
    currentOrderFilter = filter;
    ['ALL', 'OPEN', 'FILLED', 'CANCELED'].forEach(type => {
        const btn = document.getElementById(`filter${type}`);
        if (type === filter) {
            btn.style.background = 'var(--neon-blue)';
            btn.style.color = 'black';
            btn.style.border = '1px solid var(--neon-blue)';
        } else {
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
            btn.style.border = '1px solid #555';
        }
    });
    renderOrders();
}

function renderOrders() {
    const listEl = document.getElementById('orderList');
    if (!listEl) return;
    listEl.innerHTML = '';

    const orders = allMyOrders.filter(o => {
        const oType = (o.tradeType || 'CONTRACT'); // Case sensitive check might be needed if backend varies
        if (oType !== currentTradeType) return false;

        if (currentOrderFilter === 'ALL') return true;
        if (currentOrderFilter === 'OPEN') return (o.status === 'NEW' || o.status === 'PARTIAL_FILLED');
        if (currentOrderFilter === 'FILLED') return (o.status === 'FILLED' || o.status === 'PARTIAL_FILLED'); // Partial is also visible in Filled? Maybe duplicates.
        if (currentOrderFilter === 'CANCELED') return (o.status === 'CANCELED');
        return true;
    });

    if (orders.length === 0) {
        listEl.innerHTML = '<div style="text-align:center; color:#777; padding:20px;">暫無資料</div>';
        return;
    }

    orders.forEach(o => {
        const isBuy = (o.side === 'BUY');
        const color = isBuy ? '#00f3ff' : '#ff4d4d';
        const canCancel = (o.status === 'NEW' || o.status === 'PARTIAL_FILLED');
        
        let avgPrice = '-';
        if (o.filledQuantity > 0 && o.cumQuoteQty > 0) {
            avgPrice = (o.cumQuoteQty / o.filledQuantity).toFixed(4);
        }

        const div = document.createElement('div');
        div.style.background = 'rgba(255,255,255,0.05)';
        div.style.padding = '10px';
        div.style.borderRadius = '8px';
        div.style.fontSize = '0.9em';
        
        div.innerHTML = `
            <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                <span style="font-weight:bold; color:white;">${o.symbolId} <span style="font-size:0.8em; color:#bbb; border:1px solid #555; padding:0 4px; border-radius:4px; margin-left:5px;">${o.tradeType}</span></span>
                <span style="font-weight:bold; color:${color};">${o.side}</span>
            </div>
            <div style="color:#aaa; margin-bottom:5px;">
                <div>價格: ${o.price} <span style="color:var(--neon-purple); margin-left:5px;">(均價: ${avgPrice})</span></div>
                <div>數量: ${o.filledQuantity} / ${o.quantity}</div>
            </div>
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <span style="color:${getStatusColor(o.status)}">${o.status}</span>
                ${canCancel ? `<button class="btn btn-sm" style="background:#ff4d4d; width:auto; padding:3px 8px; font-size:0.8em;" onclick="cancelOrder(${o.orderId})">撤單</button>` : ''}
            </div>
        `;
        listEl.appendChild(div);
    });
}

function getStatusColor(status) {
    if (status === 'NEW') return 'white';
    if (status === 'FILLED') return '#00ff88';
    if (status === 'CANCELED') return '#777';
    return '#aaa';
}

async function cancelOrder(orderId) {
    if(!confirm('確定撤銷此訂單?')) return;
    try {
        const res = await fetch(`/api/orders/${orderId}/cancel`, { method: 'POST' });
        if (res.ok) {
            fetchMyOrders();
            fetchOrderBook();
        } else {
            alert('撤單失敗');
        }
    } catch (err) {
        console.error(err);
    }
}

// --- Order Book ---
async function fetchOrderBook() {
    const symbolId = document.getElementById('tradeSymbol').value;
    if(!symbolId) return;
    try {
        const res = await fetch(`/api/orders/book/${symbolId}?type=${currentTradeType}`);
        if (res.ok) {
            const data = await res.json();
            renderOrderBook(data);
        }
    } catch (err) {
        console.error(err);
    }
}

function renderOrderBook(data) {
    const asksEl = document.getElementById('orderBookAsks');
    const bidsEl = document.getElementById('orderBookBids');
    if (!asksEl || !bidsEl) return;
    
    asksEl.innerHTML = '';
    bidsEl.innerHTML = '';

    data.asks.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#ff4d4d', 'SELL');
        asksEl.appendChild(div);
    });

    data.bids.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#00f3ff', 'BUY');
        bidsEl.appendChild(div);
    });
}

function createBookItem(price, qty, color, itemSide) {
    const div = document.createElement('div');
    div.style.display = 'flex';
    div.style.justifyContent = 'space-between';
    div.style.fontSize = '0.85em';
    div.style.padding = '2px 5px';
    div.style.cursor = 'pointer';
    div.style.background = 'rgba(255,255,255,0.02)';
    
    div.onclick = () => {
        document.getElementById('tradePrice').value = price;
        const targetSide = (itemSide === 'BUY') ? 'SELL' : 'BUY';
        setOrderSide(targetSide);
    };
    
    div.onmouseover = () => div.style.background = 'rgba(255,255,255,0.1)';
    div.onmouseout = () => div.style.background = 'rgba(255,255,255,0.02)';

    div.innerHTML = `
        <span style="color:${color}; font-weight:bold;">${price}</span>
        <span style="color:#aaa;">${qty}</span>
    `;
    return div;
}

// --- Positions ---
async function fetchPositions() {
    try {
        const res = await fetch('/api/positions/open');
        if (res.ok) {
            const data = await res.json();
            renderPositions(data);
        }
    } catch (err) { console.error(err); }
}

function renderPositions(data) {
    const listEl = document.getElementById('positionList');
    if (!listEl) return;
    listEl.innerHTML = '';

    if (data.length === 0) {
        listEl.innerHTML = '<div style="text-align:center; color:#777; padding:10px;">暫無持倉</div>';
        return;
    }

    data.forEach(p => {
        const isLong = (p.side === 'LONG');
        const color = isLong ? '#00f3ff' : '#ff4d4d';
        
        const div = document.createElement('div');
        div.style.background = 'rgba(255,255,255,0.05)';
        div.style.padding = '10px';
        div.style.borderRadius = '8px';
        div.style.fontSize = '0.9em';
        div.style.display = 'flex';
        div.style.justifyContent = 'space-between';
        div.style.alignItems = 'center';

        div.innerHTML = `
            <div>
                <span style="font-weight:bold; color:white;">${p.symbolId}</span>
                <span style="color:${color}; margin-left:5px;">${p.side}</span>
                <div style="font-size:0.8em; color:#aaa;">
                    數量: ${p.quantity} | 均價: ${p.avgprice}
                </div>
            </div>
            <div>
                 <button class="btn btn-sm" style="background:#ff4d4d; border:none;" onclick="closePosition('${p.symbolId}', '${p.side}', ${p.quantity})">市價全平</button>
            </div>
        `;
        listEl.appendChild(div);
    });
}

async function closePosition(symbolId, side, qty) {
    if(!confirm(`確定要市價平倉 ${symbolId} ${side}?`)) return;
    const orderSide = (side === 'LONG') ? 'SELL' : 'BUY';
    alert('請使用下單區進行平倉操作 (選擇反向 + 數量)');
    document.getElementById('tradeSymbol').value = symbolId; 
    document.getElementById('tradeSymbolSearch').value = symbolId; 
    setOrderSide(orderSide);
    document.getElementById('tradeQuantity').value = qty;
}

// --- Utils (Login/Profile) ---

function clearMsgs() {
    const msgIds = ['loginMsg', 'regMsg', 'profileMsg'];
    msgIds.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.innerHTML = '';
    });
}

function showMsg(elementId, text, isError) {
    const el = document.getElementById(elementId);
    if (el) {
        el.innerHTML = text;
        el.className = isError ? 'error' : 'success';
    }
}

async function login() {
    const account = document.getElementById('loginAccount').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ account, password })
        });
        if (res.ok) {
            showDashboard();
        } else {
            showMsg('loginMsg', '拒絕存取：帳號或密碼錯誤', true);
        }
    } catch (err) {
        showMsg('loginMsg', '連線錯誤', true);
    }
}

async function register() {
    const account = document.getElementById('regAccount').value;
    const password = document.getElementById('regPassword').value;
    const name = document.getElementById('regName').value;
    const number = document.getElementById('regNumber').value;

    try {
        const res = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ account, password, name, number })
        });
        if (res.ok) {
            alert('註冊成功！請登入。');
            showLogin();
        } else {
            const txt = await res.text();
            showMsg('regMsg', '註冊失敗：' + txt, true);
        }
    } catch (err) {
        showMsg('regMsg', '連線錯誤', true);
    }
}

async function fetchSimpleProfile() {
    try {
        const res = await fetch(`${API_URL}/me`);
        if (res.ok) {
            const data = await res.json();
            const el = document.getElementById('dashName');
            if (el) el.innerText = data.name || data.account;
        } else {
            showLogin();
        }
    } catch (err) {}
}

async function fetchProfileDetail() {
    try {
        const res = await fetch(`${API_URL}/me`);
        if (res.ok) {
            const data = await res.json();
            document.getElementById('displayAccount').innerText = data.account;
            document.getElementById('viewName').innerText = data.name;
            document.getElementById('viewNumber').innerText = data.number;
            document.getElementById('updateName').value = data.name;
            document.getElementById('updateNumber').value = data.number;
        } else {
            showLogin();
        }
    } catch (err) {}
}

async function updateProfile() {
    const name = document.getElementById('updateName').value;
    const number = document.getElementById('updateNumber').value;
    const password = document.getElementById('updatePassword').value;

    try {
        const res = await fetch(`${API_URL}/me`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, number, password })
        });
        if (res.ok) {
            showMsg('profileMsg', '系統更新成功', false);
            fetchProfileDetail();
            disableEditMode();
        } else {
            showMsg('profileMsg', '更新失敗', true);
        }
    } catch (err) {
        showMsg('profileMsg', '連線錯誤', true);
    }
}

async function logout() {
    await fetch(`${API_URL}/logout`, { method: 'POST' });
    showLogin();
}

function enableEditMode() {
    document.getElementById('profilePanel').classList.add('editing');
    document.getElementById('updateName').value = document.getElementById('viewName').innerText;
    document.getElementById('updateNumber').value = document.getElementById('viewNumber').innerText;
    document.getElementById('updatePassword').value = ''; 
    clearMsgs();
}

function disableEditMode() {
    document.getElementById('profilePanel').classList.remove('editing');
    clearMsgs();
}

// --- Wallet Logic ---
let latestWallets = []; 
let latestTickers = {}; 

async function fetchWallets() {
    try {
        const [resWallets, resTickers] = await Promise.all([
            fetch(WALLET_API_URL),
            fetch('/api/symbols/tickers')
        ]);

        if (resTickers.ok) {
            latestTickers = await resTickers.json();
        }

        if (resWallets.ok) {
            latestWallets = await resWallets.json();
            renderWallets();
        } else {
            if (resWallets.status === 401) showLogin();
        }
    } catch (err) {
        console.error(err);
    }
}

function renderWallets() {
    const listEl = document.getElementById('walletList');
    if (!listEl) return;
    listEl.innerHTML = '';

    const walletMap = {};
    latestWallets.forEach(w => walletMap[w.coinId] = w);

    let displayList = SUPPORTED_COINS.map(coin => {
        const w = walletMap[coin] || { coinId: coin, balance: 0, available: 0 };
        const balance = parseFloat(w.balance || 0);
        const available = parseFloat(w.available || 0);
        let price = (coin === 'USDT') ? 1 : (latestTickers[coin] || 0);
        const usdtValue = balance * price;
        return { coinId: coin, balance: balance, available: available, usdtValue: usdtValue };
    });

    const hideZero = document.getElementById('walletHideZero').checked;
    if (hideZero) displayList = displayList.filter(w => w.balance > 0);

    const sortBy = document.getElementById('walletSortBy').value;
    const sortOrder = document.getElementById('walletSortOrder').value;
    const isAsc = sortOrder === 'ASC';

    displayList.sort((a, b) => {
        const balA = Number(a.balance);
        const balB = Number(b.balance);
        const isZeroA = (balA === 0);
        const isZeroB = (balB === 0);
        if (isZeroA && !isZeroB) return 1;
        if (!isZeroA && isZeroB) return -1;
        if (sortBy === 'NAME') {
            const idA = (a.coinId || '').toUpperCase();
            const idB = (b.coinId || '').toUpperCase();
            if (idA < idB) return isAsc ? -1 : 1;
            if (idA > idB) return isAsc ? 1 : -1;
            return 0;
        }
        let valA = (sortBy === 'BALANCE') ? balA : Number(a.usdtValue);
        let valB = (sortBy === 'BALANCE') ? balB : Number(b.usdtValue);
        return isAsc ? (valA - valB) : (valB - valA);
    });

    displayList.forEach(w => {
        const card = document.createElement('div');
        card.className = 'wallet-card';
        const valueDisplay = w.usdtValue > 0 ? `≈ ${w.usdtValue.toFixed(2)} USDT` : '';
        card.innerHTML = `
            <div class="coin-info">
                <h3>${w.coinId}</h3>
                <p>可用: ${w.available}</p>
                <p style="font-size: 0.8em; color: #aaa;">${valueDisplay}</p>
            </div>
            <div class="balance-info">
                <span class="balance-val">${w.balance}</span>
                <button class="btn btn-sm" onclick="openDepositModal('${w.coinId}')">儲值</button>
            </div>
        `;
        listEl.appendChild(card);
    });
}

function openDepositModal(coinId) {
    currentDepositCoin = coinId;
    document.getElementById('depositCoinName').innerText = coinId;
    document.getElementById('depositAmount').value = '';
    document.getElementById('depositModal').classList.remove('hidden');
}

function closeDepositModal() {
    document.getElementById('depositModal').classList.add('hidden');
    currentDepositCoin = '';
}

async function submitDeposit() {
    const amount = document.getElementById('depositAmount').value;
    if (!amount || amount <= 0) {
        alert('請輸入有效金額');
        return;
    }
    try {
        const res = await fetch(`${WALLET_API_URL}/deposit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ coinId: currentDepositCoin, amount: parseFloat(amount) })
        });
        if (res.ok) {
            alert('儲值成功！');
            closeDepositModal();
            fetchWallets(); 
        } else {
            alert('儲值失敗');
        }
    } catch (err) {
        console.error(err);
        alert('連線錯誤');
    }
}

async function resetWallets() {
    if (!confirm('確定要重置所有錢包資金嗎？')) return;
    try {
        const res = await fetch(`${WALLET_API_URL}/reset`, { method: 'POST' });
        if (res.ok) {
            alert('重置成功');
            fetchWallets();
        } else {
            alert('重置失敗');
        }
    } catch (err) { console.error(err); }
}
