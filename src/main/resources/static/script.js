const API_URL = '/api/members';
const WALLET_API_URL = '/api/wallets';
const SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB']; // 支援的預設幣種

// --- 導航與面板控制 ---

function hideAllPanels() {
    const panels = ['loginPanel', 'registerPanel', 'dashboardPanel', 'profilePanel', 'walletPanel', 'tradePanel', 'historyPanel'];
    panels.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });
    // 同時關閉所有 Modal
    closeDepositModal();
    clearMsgs();
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

function showTrade() { 
    showPanel('tradePanel'); 
    fetchMyOrders();
    fetchOrderBook();
}
function showHistory() { showPanel('historyPanel'); }

// --- 訊息處理 ---



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

// --- 會員邏輯 (Login, Register, Profile) ---

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

// --- 錢包邏輯 (Wallet) ---

let currentDepositCoin = '';

async function fetchWallets() {
    try {
        const res = await fetch(WALLET_API_URL);
        if (res.ok) {
            const myWallets = await res.json();
            renderWallets(myWallets);
        } else {
            if (res.status === 401) showLogin();
        }
    } catch (err) {
        console.error(err);
    }
}

function renderWallets(wallets) {
    const listEl = document.getElementById('walletList');
    listEl.innerHTML = '';

    // 將後端回傳的錢包轉為 Map 方便查找
    const walletMap = {};
    wallets.forEach(w => walletMap[w.coinId] = w);

    // 遍歷所有支援幣種，確保每個幣種都有顯示（即使餘額為 0）
    SUPPORTED_COINS.forEach(coin => {
        const w = walletMap[coin] || { coinId: coin, balance: 0, available: 0 };
        
        const card = document.createElement('div');
        card.className = 'wallet-card';
        card.innerHTML = `
            <div class="coin-info">
                <h3>${w.coinId}</h3>
                <p>可用: ${w.available}</p>
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
            fetchWallets(); // 刷新列表
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
            fetchWallets(); // 刷新列表
        } else {
            alert('重置失敗');
        }
    } catch (err) {
        console.error(err);
        alert('連線錯誤');
    }
}


// --- 交易邏輯 (Trade) ---

let currentOrderSide = 'BUY';

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
            // 清空輸入
            document.getElementById('tradePrice').value = '';
            document.getElementById('tradeQuantity').value = '';
            fetchMyOrders(); // 刷新訂單列表
        } else {
            const txt = await res.text();
            alert('下單失敗: ' + txt);
        }
    } catch (err) {
        console.error(err);
        alert('連線錯誤');
    }
}

async function fetchMyOrders() {
    try {
        const res = await fetch('/api/orders');
        if (res.ok) {
            allMyOrders = await res.json(); // Store globally
            renderOrders(); // Render with current filter
        }
    } catch (err) {
        console.error(err);
    }
}

// --- Order Filtering ---
let allMyOrders = [];
let currentOrderFilter = 'ALL';

function setOrderFilter(filter) {
    currentOrderFilter = filter;
    
    // Update Button Styles
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

    // Filter Logic
    const orders = allMyOrders.filter(o => {
        if (currentOrderFilter === 'ALL') return true;
        if (currentOrderFilter === 'OPEN') return (o.status === 'NEW' || o.status === 'PARTIAL_FILLED');
        if (currentOrderFilter === 'FILLED') return (o.status === 'FILLED' || o.status === 'PARTIAL_FILLED');
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
        
        // Calculate Average Price
        let avgPrice = '-';
        if (o.filledQuantity > 0 && o.cumQuoteQty > 0) {
            // Using logic: cumQuoteQty / filledQuantity
            avgPrice = (o.cumQuoteQty / o.filledQuantity).toFixed(4); // 4 decimals for crypto
        }

        const div = document.createElement('div');
        div.style.background = 'rgba(255,255,255,0.05)';
        div.style.padding = '10px';
        div.style.borderRadius = '8px';
        div.style.fontSize = '0.9em';
        
        div.innerHTML = `
            <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                <span style="font-weight:bold; color:white;">${o.symbolId}</span>
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
        } else {
            alert('撤單失敗');
        }
    } catch (err) {
        console.error(err);
    }
}

async function fetchOrderBook() {
    const symbolId = document.getElementById('tradeSymbol').value;
    try {
        const res = await fetch(`/api/orders/book/${symbolId}`);
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

    // Asks (Sorted Price ASC) -> Flex Column Reverse handles display order
    data.asks.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#ff4d4d');
        asksEl.appendChild(div);
    });

    // Bids (Sorted Price DESC)
    data.bids.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#00f3ff');
        bidsEl.appendChild(div);
    });
}

function createBookItem(price, qty, color) {
    const div = document.createElement('div');
    div.style.display = 'flex';
    div.style.justifyContent = 'space-between';
    div.style.fontSize = '0.85em';
    div.style.padding = '2px 5px';
    div.style.cursor = 'pointer';
    div.style.background = 'rgba(255,255,255,0.02)';
    
    div.onclick = () => {
        document.getElementById('tradePrice').value = price;
    };
    
    div.onmouseover = () => div.style.background = 'rgba(255,255,255,0.1)';
    div.onmouseout = () => div.style.background = 'rgba(255,255,255,0.02)';

    div.innerHTML = `
        <span style="color:${color}; font-weight:bold;">${price}</span>
        <span style="color:#aaa;">${qty}</span>
    `;
    return div;
}

// --- 初始化 ---
window.onload = async () => {
    const res = await fetch(`${API_URL}/me`);
    if(res.ok) {
        showDashboard();
    } else {
        showLogin();
    }
};
