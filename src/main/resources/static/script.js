// ====== 全域常數與設定 ======
const API_URL = '/api/members';
const WALLET_API_URL = '/api/wallets';
const ORDER_API_URL = '/api/orders';

let SUPPORTED_COINS = []; 

// ====== 全域狀態 ======
let orderBookInterval = null; 
let currentTradeType = 'SPOT'; 
let currentOrderSide = 'BUY'; 
let currentHistoryTab = 'FUNDS'; 
let currentOrderFilter = 'ALL'; 
let currentChartInterval = '1m'; 
let allMyOrders = []; 
var simChart;

window.onload = async () => {
    setupDropdown(); 
    await fetchSupportedCoins(); 
    
    try {
        const res = await fetch(`${API_URL}/me`);
        if(res.ok) {
            showDashboard(); 
        } else {
            showLogin(); 
        }
    } catch (e) {
        console.error("Session check failed", e);
        showLogin();
    }
};

async function fetchSupportedCoins() {
    try {
        SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB']; 
        const res = await fetch('/api/wallets/coins'); 
        if (res.ok) {
            SUPPORTED_COINS = await res.json();
        }
    } catch (err) {
        console.log("Using default coins");
    }
}

// ====== 導航系統 ======

function hideAllPanels() {
    const panels = ['loginPanel', 'registerPanel', 'dashboardPanel', 'profilePanel', 'walletPanel', 'tradePanel', 'simulationPanel'];
    panels.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });
    closeDepositModal();
    closeSimResult();
    clearMsgs();

    if (orderBookInterval) {
        clearInterval(orderBookInterval);
        orderBookInterval = null;
    }
    
    // 停止模擬
    if (simState.timer) clearInterval(simState.timer);
}

function showPanel(panelId) {
    hideAllPanels();
    const el = document.getElementById(panelId);
    if(el) el.classList.remove('hidden');
}

function showLogin() { showPanel('loginPanel'); }
function showRegister() { showPanel('registerPanel'); }

function showDashboard() {
    showPanel('dashboardPanel');
    fetchSimpleProfile();
}

function showProfile() {
    showPanel('profilePanel');
    fetchProfileDetails();
}

function showWallet() {
    showPanel('walletPanel');
    renderWallets();
}

function showSpot() {
    showPanel('tradePanel');
    document.getElementById('tradePanelTitle').innerText = '交易中心 (Trading Center)';
    const posSection = document.getElementById('positionSection');
    if(posSection) posSection.style.display = 'none'; 
    setTradeType('SPOT'); 
    switchHistoryTab('FUNDS');

    // === [修正] 強制圖表重新調整大小 ===
    // 給瀏覽器 50ms 渲染 DOM，確保 container 有寬度後再 resize
    setTimeout(() => {
        if (chart) {
            const container = document.getElementById('chartContainer');
            if (container) {
                chart.resize(container.clientWidth, 400);
                setOptimalView(); // 重置視野到最新
            }
        } else {
            // 如果還沒初始化過，就執行初始化
            initChart();
            fetchCandles(true);
        }
    }, 50);
}

function showSimulation() {
    showPanel('simulationPanel');
    // 重置模擬設定UI
    document.getElementById('simSetup').classList.remove('hidden');
    document.getElementById('simRun').classList.add('hidden');
    // 填充幣種選項
    const sel = document.getElementById('simSymbol');
    sel.innerHTML = '';
    SUPPORTED_COINS.filter(c => c !== 'USDT').forEach(c => {
        const opt = document.createElement('option');
        opt.value = c + 'USDT';
        opt.text = c + '/USDT';
        sel.appendChild(opt);
    });
}

function clearMsgs() {
    const ids = ['loginMsg', 'regMsg', 'profileMsg'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.innerText = '';
    });
}

// ====== 會員認證 ======

async function login() {
    const account = document.getElementById('loginAccount').value;
    const pass = document.getElementById('loginPassword').value;
    const msg = document.getElementById('loginMsg');
    
    if(!account || !pass) {
        msg.innerText = '請輸入帳號與密碼';
        msg.style.color = 'red';
        return;
    }

    try {
        const res = await fetch(`${API_URL}/login`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ account: account, password: pass })
        });
        
        if(res.ok) {
            showDashboard();
        } else {
            msg.innerText = '登入失敗：帳號或密碼錯誤';
            msg.style.color = 'red';
        }
    } catch(err) {
        msg.innerText = '連線錯誤';
        msg.style.color = 'red';
    }
}

async function logout() {
    try {
        await fetch(`${API_URL}/logout`, { method: 'POST' });
        showLogin();
    } catch(err) {
        console.error(err);
        showLogin();
    }
}

async function register() {
    const account = document.getElementById('regAccount').value;
    const pass = document.getElementById('regPassword').value;
    const name = document.getElementById('regName').value;
    const number = document.getElementById('regNumber').value;
    const msg = document.getElementById('regMsg');

    if(!account || !pass) {
        msg.innerText = '帳號密碼為必填';
        msg.style.color = 'red';
        return;
    }

    try {
        const res = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ account, password: pass, name, number })
        });
        
        if(res.ok) {
            alert('註冊成功，請登入');
            showLogin();
        } else {
            const txt = await res.text();
            msg.innerText = '註冊失敗: ' + txt;
            msg.style.color = 'red';
        }
    } catch(err) {
        msg.innerText = '連線錯誤';
        msg.style.color = 'red';
    }
}

async function fetchSimpleProfile() {
    try {
        const res = await fetch(`${API_URL}/me`);
        if(res.ok) {
            const data = await res.json();
            document.getElementById('dashName').innerText = data.name || data.account;
        }
    } catch(err) {
        console.error(err);
    }
}

// ====== 個人資料 ======

let currentUserData = null;

async function fetchProfileDetails() {
    const msg = document.getElementById('profileMsg');
    msg.innerText = '';
    disableEditMode();

    try {
        const res = await fetch(`${API_URL}/me`);
        if(res.ok) {
            const data = await res.json();
            currentUserData = data;
            
            document.getElementById('displayAccount').innerText = data.account;
            document.getElementById('viewName').innerText = data.name;
            document.getElementById('viewNumber').innerText = data.number;

            document.getElementById('updateName').value = data.name;
            document.getElementById('updateNumber').value = data.number;
            document.getElementById('updatePassword').value = '';
        } else {
            msg.innerText = '無法載入資料';
        }
    } catch(err) {
        msg.innerText = '連線錯誤';
    }
}

function enableEditMode() {
    const panel = document.getElementById('profilePanel');
    panel.classList.add('editing');
}

function disableEditMode() {
    const panel = document.getElementById('profilePanel');
    panel.classList.remove('editing');
}

async function updateProfile() {
    const name = document.getElementById('updateName').value;
    const number = document.getElementById('updateNumber').value;
    const pass = document.getElementById('updatePassword').value;
    const msg = document.getElementById('profileMsg');

    const payload = {
        name: name,
        number: number
    };
    if(pass) payload.password = pass;

    try {
        const res = await fetch(`${API_URL}/me`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if(res.ok) {
            msg.innerText = '更新成功';
            msg.style.color = 'green';
            fetchProfileDetails();
        } else {
            msg.innerText = '更新失敗';
            msg.style.color = 'red';
        }
    } catch(err) {
        msg.innerText = '連線錯誤';
        msg.style.color = 'red';
    }
}

// ====== 錢包管理 ======

let currentWallets = [];

async function renderWallets() {
    const listEl = document.getElementById('walletList');
    listEl.innerHTML = '<div style="color:#aaa;">載入中...</div>';

    try {
        const res = await fetch(WALLET_API_URL);
        if(res.ok) {
            let data = await res.json(); // DB Wallets
            
            // Merge with SUPPORTED_COINS
            const walletMap = {};
            data.forEach(w => {
                walletMap[w.coinId] = w;
            });

            let mergedWallets = SUPPORTED_COINS.map(coin => {
                if (walletMap[coin]) {
                    return walletMap[coin];
                } else {
                    return { coinId: coin, balance: 0, available: 0 };
                }
            });
            
            currentWallets = mergedWallets; 
            
            const sortBy = document.getElementById('walletSortBy').value;
            const sortOrder = document.getElementById('walletSortOrder').value;
            const hideZero = document.getElementById('walletHideZero').checked;

            let displayData = [...mergedWallets]; 

            if (hideZero) {
                displayData = displayData.filter(w => parseFloat(w.balance) > 0);
            }

            displayData.sort((a, b) => {
                let valA = parseFloat(a.balance);
                let valB = parseFloat(b.balance);
                
                if (sortBy === 'NAME') {
                    return sortOrder === 'ASC' ? a.coinId.localeCompare(b.coinId) : b.coinId.localeCompare(a.coinId);
                } 
                return sortOrder === 'ASC' ? valA - valB : valB - valA;
            });

            listEl.innerHTML = '';
            if(displayData.length === 0) {
                 listEl.innerHTML = '<div style="color:#aaa; width:100%;">無相符資產</div>';
            } else {
                displayData.forEach(w => {
                    const div = document.createElement('div');
                    div.className = 'wallet-card';
                    const frozen = parseFloat(w.balance) - parseFloat(w.available);
                    div.innerHTML = `
                        <div class="coin-title">${w.coinId}</div>
                        <div style="font-size:0.9em; color:#ccc; margin-bottom:5px;">總額: <span class="coin-balance">${parseFloat(w.balance).toFixed(4)}</span></div>
                        <div style="font-size:0.8em; color:#aaa;">可用: ${parseFloat(w.available).toFixed(4)}</div>
                        <div style="font-size:0.8em; color:#aaa; margin-bottom:10px;">凍結: ${frozen.toFixed(4)}</div>
                        <div class="action-row">
                            <button class="btn btn-sm" onclick="openDepositModal('${w.coinId}')">儲值</button>
                        </div>
                    `;
                    listEl.appendChild(div);
                });
            }

        } else {
            listEl.innerHTML = '載入失敗';
        }
    } catch(err) {
        console.error(err);
        listEl.innerHTML = '連線錯誤';
    }
}

async function resetWallets() {
    if(!confirm('確定要重置所有資產嗎？這將清空所有餘額並恢復預設值。')) return;
    try {
        const res = await fetch(`${WALLET_API_URL}/reset`, { method: 'POST' });
        if(res.ok) {
            alert('資產已重置');
            renderWallets();
        } else {
            alert('重置失敗');
        }
    } catch(err) {
        alert('錯誤');
    }
}

// --- 儲值 Modal ---
function openDepositModal(coinId) {
    document.getElementById('depositModal').classList.remove('hidden');
    document.getElementById('depositCoinName').innerText = coinId;
    document.getElementById('depositAmount').value = '';
}

function closeDepositModal() {
    const el = document.getElementById('depositModal');
    if(el) el.classList.add('hidden');
}

async function submitDeposit() {
    const coinId = document.getElementById('depositCoinName').innerText;
    const amount = document.getElementById('depositAmount').value;
    
    if(!amount || amount <= 0) {
        alert('請輸入有效金額');
        return;
    }

    try {
        const res = await fetch(`${WALLET_API_URL}/deposit`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({ coinId: coinId, amount: parseFloat(amount) })
        });
        if(res.ok) {
            alert('儲值成功');
            closeDepositModal();
            renderWallets();
        } else {
            alert('儲值失敗');
        }
    } catch(err) {
        alert('錯誤');
    }
}


// ====== 交易核心邏輯 ======

let symbolOptions = [];

function setupDropdown() {
    const searchInput = document.getElementById('tradeSymbolSearch');
    const dropdownList = document.getElementById('tradeSymbolList');
    const hiddenInput = document.getElementById('tradeSymbol');

    if(!searchInput || !dropdownList) return;

    searchInput.addEventListener('click', (e) => {
        e.stopPropagation();
        renderSymbolDropdown(); 
        dropdownList.classList.remove('hidden');
    });

    searchInput.addEventListener('input', () => {
        renderSymbolDropdown(searchInput.value);
    });

    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !dropdownList.contains(e.target)) {
            dropdownList.classList.add('hidden');
        }
    });
}

function renderSymbolDropdown(filterText = '') {
    const dropdownList = document.getElementById('tradeSymbolList');
    dropdownList.innerHTML = '';

    const filter = filterText.toUpperCase();
    const filtered = symbolOptions.filter(opt => opt.value.includes(filter) || opt.text.includes(filter));

    if (filtered.length === 0) {
        dropdownList.innerHTML = '<div style="padding:10px; color:#aaa;">無相符項目</div>';
        return;
    }

    filtered.forEach(opt => {
        const div = document.createElement('div');
        div.className = 'dropdown-item';
        div.innerText = opt.value; 
        div.onclick = () => {
            selectSymbol(opt.value, opt.text);
        };
        dropdownList.appendChild(div);
    });
}

function selectSymbol(value, text, fetchNow = true) {
    const searchInput = document.getElementById('tradeSymbolSearch');
    const hiddenInput = document.getElementById('tradeSymbol');
    const dropdownList = document.getElementById('tradeSymbolList');

    searchInput.value = value;
    hiddenInput.value = value;
    dropdownList.classList.add('hidden');
    
    if(fetchNow) {
        fetchMyOrders();
        fetchOrderBook();
        initChart();
        fetchCandles();
    }
}


function setTradeType(type) {
    if (type === 'CONTRACT') {
        console.warn("Contract trading is disabled.");
        type = 'SPOT'; 
    }
    
    currentTradeType = type;
    
    const label = document.getElementById('labelSymbol');
    const hiddenInput = document.getElementById('tradeSymbol'); 
    const currentVal = hiddenInput.value;
    
    symbolOptions = [];
    const coins = SUPPORTED_COINS.filter(c => c !== 'USDT');
    
    if(label) label.innerText = '交易對 (Pair)';
    coins.forEach(coin => {
        symbolOptions.push({
            value: coin + 'USDT',
            text: coin
        });
    });

    const found = symbolOptions.find(o => o.value === currentVal);
    if (found) {
        selectSymbol(found.value, found.text, false);
    } else if (symbolOptions.length > 0) {
        selectSymbol(symbolOptions[0].value, symbolOptions[0].text, false);
    }
    
    fetchMyOrders();
    fetchOrderBook();
    initChart();
    fetchCandles();
    
    if (orderBookInterval) clearInterval(orderBookInterval);
    orderBookInterval = setInterval(() => {
        fetchOrderBook();
        fetchCandles();
    }, 2000);
}

function setOrderSide(side) {
    currentOrderSide = side;
    
    const btnBuy = document.getElementById('btnBuy');
    const btnSell = document.getElementById('btnSell');
    
    if (side === 'BUY') {
        btnBuy.style.background = 'var(--neon-blue)';
        btnBuy.style.color = 'black';
        btnSell.style.background = 'transparent';
        btnSell.style.color = 'var(--star-white)';
    } else {
        btnSell.style.background = '#ff4d4d';
        btnSell.style.color = 'black';
        btnBuy.style.background = 'transparent';
        btnBuy.style.color = 'var(--star-white)';
    }
}

function togglePriceInput() {
    const type = document.getElementById('tradeType').value;
    const priceInput = document.getElementById('tradePrice');
    
    if (type === 'MARKET') {
        priceInput.disabled = true;
        priceInput.placeholder = '市價 (Market)';
        priceInput.value = '';
    } else {
        priceInput.disabled = false;
        priceInput.placeholder = '輸入價格';
    }
}

async function submitOrder() {
    const symbolId = document.getElementById('tradeSymbol').value;
    const type = document.getElementById('tradeType').value;
    const quantity = document.getElementById('tradeQuantity').value;
    
    let price = document.getElementById('tradePrice').value;

    if (!quantity || quantity <= 0) {
        alert('請輸入有效的數量');
        return;
    }
    
    if (type === 'LIMIT' && (!price || price <= 0)) {
        alert('限價單請輸入有效價格');
        return;
    }

    const payload = {
        symbolId: symbolId,
        side: currentOrderSide,
        tradeType: currentTradeType,
        type: type,
        quantity: parseFloat(quantity)
    };
    
    if (type === 'LIMIT') {
        payload.price = parseFloat(price);
    } else {
        payload.price = 0; 
    }

    try {
        const res = await fetch(ORDER_API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const data = await res.json();
            alert(`下單成功！單號: ${data.orderId}`);
            if (type === 'LIMIT') {
                document.getElementById('tradePrice').value = '';
            }
            document.getElementById('tradeQuantity').value = '';
            fetchMyOrders(); 
            fetchOrderBook(); 
        } else {
            const txt = await res.text();
            alert('下單失敗: ' + txt);
        }
    } catch (err) {
        console.error(err);
        alert('連線錯誤');
    }
}


// ====== K 線圖表模組 ======
let chart = null;
let candleSeries = null;

function setChartInterval(interval) {
    currentChartInterval = interval;
    
    const buttons = document.querySelectorAll('#chartTimeframeGroup button');
    
    if (buttons.length > 0) {
        buttons.forEach(btn => {
            btn.className = 'btn btn-sm btn-secondary'; 
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
            
            // 忽略文字前後空白進行比對
            if (btn.textContent.trim() === interval) {
                btn.className = 'btn btn-sm';
                btn.style.background = 'var(--neon-blue)';
                btn.style.color = 'black';
            }
        });
    }
    
    // 切換週期時，傳入 true 強制重置視野
    fetchCandles(true); 
}

function initChart() {
    const container = document.getElementById('chartContainer');
    if (!container) return;
    
    // 如果圖表已存在，先移除舊的，防止重複建立
    if (chart) {
        chart.remove();
        chart = null;
    }

    // 建立圖表 (v4 版本語法)
    chart = LightweightCharts.createChart(container, {
        width: container.clientWidth,
        height: 400,
        layout: {
            background: { 
                type: 'solid', 
                color: 'transparent' // 設定為透明，這樣才會吃到 div 的背景色
            },
            textColor: '#d1d4f9',    // 設定文字顏色，避免看不清楚
        },
        grid: {
            vertLines: { color: 'rgba(255, 255, 255, 0.1)' },
            horzLines: { color: 'rgba(255, 255, 255, 0.1)' },
        },
        timeScale: {
            timeVisible: true,
            secondsVisible: false,
            borderColor: 'rgba(255, 255, 255, 0.2)',
        },
        rightPriceScale: {
            borderColor: 'rgba(255, 255, 255, 0.2)',
            autoScale: true, // Y軸自動縮放
        },
    });

    candleSeries = chart.addCandlestickSeries({
        upColor: '#00ff88',
        downColor: '#ff4d4d',
        borderDownColor: '#ff4d4d',
        borderUpColor: '#00ff88',
        wickDownColor: '#ff4d4d',
        wickUpColor: '#00ff88',
    });

    window.addEventListener('resize', () => {
        if (chart && container) {
            chart.resize(container.clientWidth, 400);
        }
    });
}

async function fetchCandles(shouldResetZoom = false) {
    const symbolId = document.getElementById('tradeSymbol').value || 'BTCUSDT';
    if (!candleSeries) return;

    const binanceSymbol = symbolId.toUpperCase();

    try {
        const res = await fetch(`/api/candles/proxy/${binanceSymbol}?interval=${currentChartInterval}`);
        if (res.ok) {
            const data = await res.json();
            if (Array.isArray(data) && data.length > 0) {
                const chartData = data.map(d => ({
                    time: d[0] / 1000 + (8 * 3600), // UTC+8
                    open: parseFloat(d[1]),
                    high: parseFloat(d[2]),
                    low: parseFloat(d[3]),
                    close: parseFloat(d[4])
                }));
                chartData.sort((a, b) => a.time - b.time);

                // === [新增] 智能價格精度調整 ===
                const lastPrice = chartData[chartData.length - 1].close;
                let precision = 2;
                let minMove = 0.01;

                if (lastPrice < 1) { 
                    precision = 6; minMove = 0.000001; // 如 DOGE, SHIB
                } else if (lastPrice < 10) { 
                    precision = 4; minMove = 0.0001; 
                } else if (lastPrice > 1000) { 
                    precision = 2; minMove = 0.01; // 如 BTC
                }

                candleSeries.applyOptions({
                    priceFormat: { type: 'price', precision: precision, minMove: minMove },
                });
                // ==============================

                candleSeries.setData(chartData);

                // 如果是切換週期或幣種，就執行最佳視野調整
                if (shouldResetZoom) {
                    setOptimalView();
                }
                return;
            }
        }
        throw new Error("Invalid API response");
    } catch (err) {
        console.warn("Fetch candles failed, using dummy data:", err);
        generateDummyChartData(); 
        if (shouldResetZoom) {
            setOptimalView();
        }
    }
}

function generateDummyChartData() {
    const data = [];
    let time = Math.floor(Date.now() / 1000) - 1000 * 60;
    let price = 100;
    
    for (let i = 0; i < 100; i++) {
        const move = (Math.random() - 0.5) * 2;
        const open = price;
        const close = price + move;
        const high = Math.max(open, close) + Math.random();
        const low = Math.min(open, close) - Math.random();
        
        data.push({
            time: time + i * 60,
            open: open,
            high: high,
            low: low,
            close: close
        });
        price = close;
    }
    candleSeries.setData(data);
}


// ====== 訂單列表 ======

async function fetchMyOrders() {
    try {
        const res = await fetch(ORDER_API_URL);
        if (res.ok) {
            allMyOrders = await res.json();
            renderOrders();
        }
    } catch (err) {
        console.error(err);
    }
}

function setOrderFilter(filter) {
    currentOrderFilter = filter;
    
    ['ALL', 'OPEN', 'FILLED', 'CANCELED'].forEach(f => {
        const btn = document.getElementById('filter' + f);
        if(btn) {
            if(f === filter) {
                btn.style.background = 'var(--neon-blue)';
                btn.style.color = 'black';
                btn.style.borderColor = 'var(--neon-blue)';
            } else {
                btn.style.background = 'transparent';
                btn.style.color = 'var(--star-white)';
                btn.style.borderColor = '#555';
            }
        }
    });

    renderOrders();
}

function renderOrders() {
    const listEl = document.getElementById('orderList');
    if (!listEl) return;
    listEl.innerHTML = '';

    const orders = allMyOrders.filter(o => {
        const oType = (o.tradeType || 'SPOT'); 
        if (oType !== currentTradeType) return false;

        if (currentOrderFilter === 'ALL') return true;
        if (currentOrderFilter === 'OPEN') return (o.status === 'NEW' || o.status === 'PARTIAL_FILLED');
        if (currentOrderFilter === 'FILLED') return (o.status === 'FILLED');
        if (currentOrderFilter === 'CANCELED') return (o.status === 'CANCELED');
        return true;
    });
    
    if (orders.length === 0) {
        listEl.innerHTML = '<div style="text-align:center; color:#777; padding:10px;">無符合訂單</div>';
        return;
    }

    orders.forEach(o => {
        const div = document.createElement('div');
        div.className = 'order-card'; 
        div.style.background = 'rgba(255,255,255,0.05)';
        div.style.padding = '10px';
        div.style.borderRadius = '5px';
        
        const sideColor = o.side === 'BUY' ? '#00ff88' : '#ff4d4d';
        const dateStr = new Date(o.createdAt).toLocaleString();
        
        let actionBtn = '';
        if (o.status === 'NEW' || o.status === 'PARTIAL_FILLED') {
            actionBtn = `<button class="btn btn-sm" onclick="cancelOrder(${o.orderId})" style="background:#555; font-size:0.8em;">撤單</button>`;
        }

        // === [修正] 市價單顯示邏輯 ===
        let displayPrice = o.price;
        if (o.type && o.type.toUpperCase() === 'MARKET') {
            const filled = parseFloat(o.filledQuantity);
            const cumQty = parseFloat(o.cumQuoteQty);
            if (filled > 0 && cumQty > 0) {
                 const avg = cumQty / filled;
                 // 簡單判斷精度
                 let p = 2;
                 if(avg < 10) p = 4;
                 if(avg < 0.1) p = 6;
                 displayPrice = avg.toFixed(p) + ' (均價)';
            } else {
                 displayPrice = '市價';
            }
        }
        // ==========================

        div.innerHTML = `
            <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                <span style="font-weight:bold; color:${sideColor}">${o.side} ${o.symbolId}</span>
                <span style="font-size:0.8em; color:#aaa;">${o.status}</span>
            </div>
            <div style="font-size:0.9em; display:flex; justify-content:space-between;">
                <span>價格: ${displayPrice}</span>
                <span>數量: ${o.quantity}</span>
            </div>
            <div style="font-size:0.8em; color:#777; margin-top:5px; display:flex; justify-content:space-between; align-items:center;">
                <span>${dateStr}</span>
                ${actionBtn}
            </div>
        `;
        listEl.appendChild(div);
    });
}

async function cancelOrder(orderId) {
    if(!confirm('確定要撤銷此訂單嗎？')) return;
    try {
        const res = await fetch(`${ORDER_API_URL}/${orderId}/cancel`, { method: 'POST' });
        if(res.ok) {
            alert('已撤單');
            fetchMyOrders();
            fetchOrderBook();
        } else {
            alert('撤單失敗');
        }
    } catch(err) {
        alert('錯誤');
    }
}


// ====== 訂單簿 (Order Book) ======
async function fetchOrderBook() {
    const symbolId = document.getElementById('tradeSymbol').value;
    if(!symbolId) return;
    try {
        const res = await fetch(`${ORDER_API_URL}/book/${symbolId}?type=${currentTradeType}`);
        if (res.ok) {
            const data = await res.json();
            renderOrderBook(data);
        }
    } catch (err) {
        // console.error(err);
    }
}

function renderOrderBook(data) {
    const asksEl = document.getElementById('orderBookAsks');
    const bidsEl = document.getElementById('orderBookBids');
    if (!asksEl || !bidsEl) return;
    
    asksEl.innerHTML = '';
    bidsEl.innerHTML = '';

    // Asks
    let asks = [...data.asks].sort((a,b) => a.price - b.price); 
    asks = asks.slice(0, 12); 
    
    asks.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#ff4d4d', 'SELL');
        asksEl.appendChild(div);
    });

    // Bids
    let bids = [...data.bids].sort((a,b) => a.price - b.price); 
    let bestBids = [...data.bids].sort((a,b) => b.price - a.price).slice(0, 12);
    bestBids.sort((a,b) => a.price - b.price);
    
    bestBids.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#00f3ff', 'BUY');
        bidsEl.appendChild(div);
    });

    const bestAsk = asks.length > 0 ? asks[0].price : '---';
    const bestBid = bestBids.length > 0 ? bestBids[bestBids.length - 1].price : '---';

    const askEl = document.getElementById('bestAskPrice');
    if(askEl) askEl.innerText = bestAsk;
    
    const bidEl = document.getElementById('bestBidPrice');
    if(bidEl) bidEl.innerText = bestBid;
}

function createBookItem(price, qty, color, itemSide) {
    const div = document.createElement('div');
    div.style.display = 'flex';
    div.style.justifyContent = 'space-between';
    div.style.fontSize = '0.9em';
    div.style.cursor = 'pointer';
    div.className = 'book-item'; 
    
    div.innerHTML = `
        <span style="color:${color}">${price}</span>
        <span style="color:#ccc">${qty}</span>
    `;
    
    div.onclick = () => {
        document.getElementById('tradePrice').value = price;
        const targetSide = (itemSide === 'BUY') ? 'SELL' : 'BUY';
        setOrderSide(targetSide);
    };
    return div;
}


// ====== 歷史紀錄 ======

function switchHistoryTab(tab) {
    currentHistoryTab = tab;
    
    const tabs = { 'FUNDS': 'tabHistoryFunds', 'TRADES': 'tabHistoryTrades' };
    
    for(let k in tabs) {
        const btn = document.getElementById(tabs[k]);
        if(k === tab) {
            btn.style.background = 'var(--neon-blue)';
            btn.style.color = 'black';
        } else {
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
        }
    }

    refreshHistory();
}

function refreshHistory() {
    const content = document.getElementById('historyContent');
    content.innerHTML = '<div style="text-align:center; padding:10px;">載入中...</div>';

    if (currentHistoryTab === 'FUNDS') fetchHistoryFunds();
    else if (currentHistoryTab === 'TRADES') fetchHistoryTrades();
}

async function fetchHistoryFunds() {
    try {
        const res = await fetch(`${WALLET_API_URL}/transactions`);
        if(res.ok) {
            const data = await res.json();
            renderHistoryFunds(data);
        }
    } catch(err) {
        document.getElementById('historyContent').innerHTML = '載入錯誤';
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
                    <th style="padding:8px;">時間</th>
                    <th style="padding:8px;">類型</th>
                    <th style="padding:8px;">幣種</th>
                    <th style="padding:8px; text-align:right;">金額</th>
                </tr>
            </thead>
            <tbody>
    `;

    data.forEach(item => {
        const date = new Date(item.createdAt).toLocaleString();
        const color = item.amount >= 0 ? '#00ff88' : '#ff4d4d';
        
        html += `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:8px;">${date}</td>
                <td style="padding:8px;">${item.type}</td>
                <td style="padding:8px;">${item.coinId}</td>
                <td style="padding:8px; text-align:right; color:${color};">${item.amount}</td>
            </tr>
        `;
    });
    html += '</tbody></table>';
    el.innerHTML = html;
}

async function fetchHistoryTrades() {
    try {
        const res = await fetch(`${ORDER_API_URL}/trades`);
        if(res.ok) {
            const data = await res.json();
            const filtered = data.filter(t => t.tradeType === 'SPOT');
            renderHistoryTrades(filtered);
        }
    } catch(err) {
        document.getElementById('historyContent').innerHTML = '載入錯誤';
    }
}

function renderHistoryTrades(data) {
    const el = document.getElementById('historyContent');
    if (!data || data.length === 0) {
        el.innerHTML = '<div style="text-align:center; padding:20px; color:#777;">暫無成交紀錄</div>';
        return;
    }

    let html = `
        <table style="width:100%; border-collapse:collapse; font-size:0.9em;">
            <thead>
                <tr style="border-bottom:1px solid #555; color:#aaa;">
                    <th style="padding:8px;">時間</th>
                    <th style="padding:8px;">交易對</th>
                    <th style="padding:8px;">方向</th>
                    <th style="padding:8px; text-align:right;">價格</th>
                    <th style="padding:8px; text-align:right;">數量</th>
                </tr>
            </thead>
            <tbody>
    `;

    data.forEach(item => {
        const date = new Date(item.executedAt).toLocaleString();
        const sideColor = item.side === 'BUY' ? '#00ff88' : '#ff4d4d';
        
        html += `
            <tr style="border-bottom:1px solid rgba(255,255,255,0.05);">
                <td style="padding:8px;">${date}</td>
                <td style="padding:8px;">${item.symbolId}</td>
                <td style="padding:8px; color:${sideColor};">${item.side}</td>
                <td style="padding:8px; text-align:right;">${item.price}</td>
                <td style="padding:8px; text-align:right;">${item.quantity}</td>
            </tr>
        `;
    });
    html += '</tbody></table>';
    el.innerHTML = html;
}

function fetchPositions() {
}

// ====== 模擬回測系統 (Simulation Engine) ======

const simState = {
    usdt: 10000,
    coinQty: 0,
    symbol: '',
    data: [], // K線數據
    currentIndex: 0,
    isPlaying: false,
    speed: 1000, // ms per candle
    timer: null,
    chart: null,
    series: null,
    maxEquity: 10000, // 追蹤最大淨值
    tradeCount: 0,
    winCount: 0,
    initialEquity: 10000
};

async function startSimulation() {
    const symbol = document.getElementById('simSymbol').value;
    const interval = document.getElementById('simInterval').value;
    const startTimeStr = document.getElementById('simStartTime').value;
    
    // 初始化狀態
    simState.symbol = symbol;
    simState.usdt = 10000;
    simState.coinQty = 0;
    simState.currentIndex = 0;
    simState.isPlaying = false;
    simState.maxEquity = 10000;
    simState.tradeCount = 0;
    simState.winCount = 0;
    
    // 計算 startTime (Long timestamp)
    let startTime = null;
    if (startTimeStr) {
        startTime = new Date(startTimeStr).getTime();
    }

    // 顯示載入中
    document.getElementById('simSetup').classList.add('hidden');
    document.getElementById('simRun').classList.remove('hidden');
    
    // 初始化圖表
    initSimChart();
    
    // 抓取資料
    try {
        let url = `/api/candles/proxy/${symbol}?interval=${interval}`;
        if (startTime) url += `&startTime=${startTime}`;
        
        const res = await fetch(url);
        if (res.ok) {
            const rawData = await res.json();
            // Binance Format -> Lightweight Charts Format
            simState.data = rawData.map(d => ({
                time: d[0] / 1000 + (8*3600),
                open: parseFloat(d[1]),
                high: parseFloat(d[2]),
                low: parseFloat(d[3]),
                close: parseFloat(d[4])
            })).sort((a,b) => a.time - b.time);
            
            if (simState.data.length < 50) {
                alert('數據不足，無法模擬');
                finishSimulation();
                return;
            }

            // 預載前 50 根作為背景
            simState.currentIndex = 50;
            const preload = simState.data.slice(0, simState.currentIndex);
            simState.series.setData(preload);
            
            updateSimDashboard();
            toggleSimPause(); // 自動開始播放
        } else {
            alert('無法獲取歷史資料');
            finishSimulation();
        }
    } catch (e) {
        console.error(e);
        alert('發生錯誤');
        finishSimulation();
    }
}

function initSimChart() {
    const container = document.getElementById('simChartContainer');
    container.innerHTML = ''; // Clear old chart
    
    simState.chart = LightweightCharts.createChart(container, {
        width: container.clientWidth,
        height: 400,
        layout: {
        background: { 
            type: 'solid', 
            color: 'transparent'  // 關鍵！設定透明，讓它吃到 HTML 的深色背景
            // 如果透明沒效，請試試改成強制深色： color: '#151520' 
        },
        textColor: '#d1d4f9',     // 文字顏色改成淡紫色，才看得到座標
    },
    // ▲▲▲ 修改結束 ▲▲▲

    // 順便把網格線調暗，不然在黑底上會太亮
    grid: {
        vertLines: { color: 'rgba(42, 46, 57, 0.2)' },
        horzLines: { color: 'rgba(42, 46, 57, 0.2)' },
    },
    rightPriceScale: {
        visible: true,           // 確保它是開啟的
        borderVisible: true,     // 顯示右側的分隔線
        borderColor: 'rgba(255, 255, 255, 0.1)', // 分隔線顏色
        scaleMargins: {
            top: 0.1,            // 上方留白 10%
            bottom: 0.1,         // 下方留白 10%
        },
    },
    timeScale: {
        borderColor: 'rgba(255, 255, 255, 0.1)', // 下方時間軸的分隔線也順便加深
        timeVisible: true,
        secondsVisible: false,
        },
    });
    
    simChart = simState.chart;

    simState.series = simState.chart.addCandlestickSeries({
        upColor: '#00ff88',
        downColor: '#ff4d4d',
        borderDownColor: '#ff4d4d',
        borderUpColor: '#00ff88',
        wickDownColor: '#ff4d4d',
        wickUpColor: '#00ff88',
    });
}

function toggleSimPause() {
    const btn = document.getElementById('btnSimPause');
    
    if (simState.isPlaying) {
        // 暫停
        simState.isPlaying = false;
        clearInterval(simState.timer);
        btn.innerText = '繼續';
        btn.style.background = 'var(--neon-blue)';
    } else {
        // 播放
        simState.isPlaying = true;
        simState.timer = setInterval(playStep, simState.speed);
        btn.innerText = '暫停';
        btn.style.background = '#ff4d4d';
    }
}

function updateSimSpeed(val) {
    simState.speed = parseInt(val);
    // 顯示速度 (ms 轉 秒)
    const sec = (simState.speed / 1000).toFixed(1);
    document.getElementById('speedLabel').innerText = sec + 's/根';
    
    // 如果正在播放，重啟計時器以應用新速度
    if (simState.isPlaying) {
        clearInterval(simState.timer);
        simState.timer = setInterval(playStep, simState.speed);
    }
}

function playStep() {
    if (simState.currentIndex >= simState.data.length) {
        finishSimulation();
        return;
    }
    
    // 取出下一根 K 線
    const candle = simState.data[simState.currentIndex];
    simState.series.update(candle);
    
    simState.currentIndex++;
    updateSimDashboard(candle.close);
}

function updateSimDashboard(currentPrice) {
    if (!currentPrice) {
        const lastCandle = simState.data[simState.currentIndex - 1];
        currentPrice = lastCandle ? lastCandle.close : 0;
    }
    
    const equity = simState.usdt + (simState.coinQty * currentPrice);
    
    // 更新最大回撤紀錄
    if (equity > simState.maxEquity) {
        simState.maxEquity = equity;
    }
    
    // 計算未實現盈虧 (相對於初始資金)
    const pnl = equity - simState.initialEquity;
    const pnlPct = (pnl / simState.initialEquity) * 100;
    
    document.getElementById('simEquity').innerText = equity.toFixed(2);
    document.getElementById('simHoldings').innerText = simState.coinQty.toFixed(4);
    
    const pnlEl = document.getElementById('simPnL');
    pnlEl.innerText = (pnl >= 0 ? '+' : '') + pnlPct.toFixed(2) + '%';
    pnlEl.style.color = pnl >= 0 ? '#00ff88' : '#ff4d4d';
    
    document.getElementById('simProgress').innerText = `${simState.currentIndex} / ${simState.data.length}`;
}

function simAction(side) {
    if (!simState.isPlaying && simState.currentIndex >= simState.data.length) return;
    
    // 取得當前價格 (上一根收盤價)
    const currentPrice = simState.data[simState.currentIndex - 1].close;
    
    if (side === 'BUY') {
        // 全倉買入
        if (simState.usdt > 0) {
            const buyQty = simState.usdt / currentPrice;
            simState.coinQty += buyQty; // 簡化：不扣手續費
            simState.usdt = 0;
            // 記錄交易
            simState.tradeCount++;
        }
    } else {
        // 全倉賣出
        if (simState.coinQty > 0) {
            const sellValue = simState.coinQty * currentPrice;
            // 判斷這筆交易是否獲利 (簡化：若賣出後資產增加視為獲利，但這裡應該追蹤買入成本)
            // 這裡用簡單的 "賣出時資產 > 上次操作資產" 太複雜，
            // 改為：若賣出後總資產 > 初始 10000 則勝率 + 1 (這定義怪怪的)
            // 正確做法：FIFO 成本計算。這裡簡化：只要賣出時總資產比 10000 高就算贏了一次？
            // 為了 Demo，我們簡化定義：只要賣出動作發生，且賣出價格 > 0 就計次。
            // 真正的勝率需要追蹤每一筆 Trade 的 Entry Price。
            // 這裡暫時只計算 "總資產 > 10000" 代表最終勝利。
            
            // 更好的簡化：假設每次買入都是一筆 Trade 的開始，賣出是結束。
            // 紀錄買入均價。
            
            simState.usdt += sellValue;
            simState.coinQty = 0;
            simState.tradeCount++;
        }
    }
    updateSimDashboard(currentPrice);
}

function finishSimulation() {
    clearInterval(simState.timer);
    simState.isPlaying = false;
    
    // 強制平倉
    const lastPrice = simState.data[simState.currentIndex - 1].close;
    if (simState.coinQty > 0) {
        simState.usdt += simState.coinQty * lastPrice;
        simState.coinQty = 0;
    }
    
    // 計算結果
    const finalEquity = simState.usdt;
    const totalPnL = finalEquity - simState.initialEquity;
    
    // 最大回撤計算: (MaxPeak - Final) / MaxPeak (簡化版，應該是過程中最大的跌幅)
    // 這裡我們只追蹤了 maxEquity。如果現在比 max 低，就是回撤。
    // 正確回撤應該是在過程中持續計算。
    // 這裡用簡單公式： (歷史最高 - 最終) / 歷史最高
    const drawdown = (simState.maxEquity - finalEquity) / simState.maxEquity * 100;
    
    // 顯示結果
    document.getElementById('resFinalEquity').innerText = finalEquity.toFixed(2);
    document.getElementById('resTotalPnL').innerText = totalPnL.toFixed(2);
    document.getElementById('resTradeCount').innerText = simState.tradeCount;
    
    // 勝率暫時用 "是否賺錢" 代表
    document.getElementById('resWinRate').innerText = totalPnL > 0 ? '100%' : '0%'; 
    if(simState.tradeCount > 0) {
         // 若有多次交易，這裡數據可能不準，標記為 Demo
         document.getElementById('resWinRate').innerText = '(Demo)'; 
    }

    document.getElementById('resMaxDrawdown').innerText = drawdown.toFixed(2) + '%';
    
    document.getElementById('simResultModal').classList.remove('hidden');
}

function closeSimResult() {
    document.getElementById('simResultModal').classList.add('hidden');
}

function setOptimalView() {
    if (!chart || !candleSeries) return;

    const data = candleSeries.data();
    if (data.length === 0) return;

    // 設定顯示最近 80 根
    const visibleCandles = 80;
    const totalLength = data.length;
    const fromIndex = totalLength - visibleCandles;
    const toIndex = totalLength + 5; // 右邊留白 5 根

    setTimeout(() => {
        // 1. 設定 X 軸範圍
        chart.timeScale().setVisibleLogicalRange({
            from: fromIndex,
            to: toIndex
        });

        // 2. 強制 Y 軸根據目前畫面自動縮放 (Auto Scale)
        chart.priceScale('right').applyOptions({
            autoScale: true,
            scaleMargins: {
                top: 0.1,    // 上方留白 10%
                bottom: 0.1, // 下方留白 10%
            },
        });
        
        // 3. 確保捲動到最右邊
        chart.timeScale().scrollToPosition(5, true); 
    }, 10);
}

function resetSimView() {
    // 檢查 simChart 是否存在 (simChart 是你建立圖表時的全域變數)
    if (typeof simChart !== 'undefined' && simChart) {
        // Lightweight Charts 的重置語法
        simChart.timeScale().fitContent();
        
        // 或者是回到最新一根 K 線 (如果你比較喜歡這種效果，可用下面這行取代上面那行)
        // simChart.timeScale().scrollToRealTime(); 
    }
}