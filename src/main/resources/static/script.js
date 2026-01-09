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
let currentChartInterval = '1m'; // Default interval
let allMyOrders = []; 

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
        const res = await fetch('/api/wallets/coins'); 
        if (res.ok) {
            SUPPORTED_COINS = await res.json(); 
        } else {
            console.warn("Failed to fetch coins, using defaults");
            SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB'];
        }
    } catch (err) {
        console.log("Using default coins");
        SUPPORTED_COINS = ['USDT', 'BTC', 'ETH', 'BNB'];
    }
}

// ====== 導航系統 ======

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

    // === [新增修正] 強制圖表重新調整大小 ===
    // 使用 setTimeout 確保 HTML 元素已經移除 hidden class 並完成渲染後才執行
    setTimeout(() => {
        if (chart) {
            const container = document.getElementById('chartContainer');
            // 強制圖表適應目前容器的寬高
            chart.resize(container.clientWidth, 400); 
            // 讓 K 線圖自動縮放以填滿畫面
            chart.timeScale().fitContent(); 
        }
    }, 0);
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
            
            // [Fix]: Merge with SUPPORTED_COINS to ensure all coins are listed
            const walletMap = {};
            data.forEach(w => {
                walletMap[w.coinId] = w;
            });

            // Rebuild the list based on supported coins
            let mergedWallets = SUPPORTED_COINS.map(coin => {
                if (walletMap[coin]) {
                    return walletMap[coin];
                } else {
                    // Virtual 0-balance wallet
                    return { coinId: coin, balance: 0, available: 0 };
                }
            });
            
            currentWallets = mergedWallets; 
            
            // 排序與過濾
            const sortBy = document.getElementById('walletSortBy').value;
            const sortOrder = document.getElementById('walletSortOrder').value;
            const hideZero = document.getElementById('walletHideZero').checked;

            let displayData = [...mergedWallets]; 

            if (hideZero) {
                // Ensure balance is number
                displayData = displayData.filter(w => parseFloat(w.balance) > 0);
            }

            displayData.sort((a, b) => {
                let valA = parseFloat(a.balance);
                let valB = parseFloat(b.balance);
                
                if (sortBy === 'NAME') {
                    return sortOrder === 'ASC' ? a.coinId.localeCompare(b.coinId) : b.coinId.localeCompare(a.coinId);
                } 
                // Default: BALANCE
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
    
    console.log("Starting Polling for " + type);
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

// [New] Toggle Price Input based on Order Type
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
    
    // For LIMIT order, price is required
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
        // Market order: price is ignored or 0
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


// ====== K 線圖表模組 (Chart Module) ======
let chart = null;
let candleSeries = null;

// [New] Set Chart Interval
function setChartInterval(interval) {
    currentChartInterval = interval;
    
    // Select buttons within the specific group by ID
    const buttons = document.querySelectorAll('#chartTimeframeGroup button');
    
    if (buttons.length > 0) {
        buttons.forEach(btn => {
            // Reset to default style first
            btn.className = 'btn btn-sm btn-secondary'; 
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
            
            // Apply active style if text matches
            // Use textContent to ignore CSS text-transform: uppercase
            if (btn.textContent.trim() === interval) {
                btn.className = 'btn btn-sm';
                btn.style.background = 'var(--neon-blue)';
                btn.style.color = 'black';
            }
        });
    }
    
    fetchCandles(true); // Reload data
}

function initChart() {
    const container = document.getElementById('chartContainer');
    if (!container) return;
    
    // 如果圖表已存在，先移除舊的 (避免重複建立導致記憶體洩漏或顯示錯誤)
    if (chart) {
        chart.remove();
        chart = null;
    }

    // 建立圖表實例
    chart = LightweightCharts.createChart(container, {
        width: container.clientWidth, // 設定寬度為容器當前寬度
        height: 400, // 設定高度
        // --- [重點修正] Layout 設定 (適配 v4 版本) ---
        layout: {
            background: { type: 'solid', color: 'transparent' }, // 背景設為透明，讓網頁的深色背景透出來
            textColor: '#D9D9D9', // 設定文字顏色為淺灰色 (確保看得見)
        },
        // -------------------------------------------
        grid: {
            vertLines: { color: 'rgba(255, 255, 255, 0.1)' }, // 垂直網格線：微透明白
            horzLines: { color: 'rgba(255, 255, 255, 0.1)' }, // 水平網格線：微透明白
        },
        timeScale: {
            timeVisible: true,
            secondsVisible: false,
            borderColor: 'rgba(255, 255, 255, 0.2)', // X軸邊框顏色
        },
        rightPriceScale: {
            borderColor: 'rgba(255, 255, 255, 0.2)', // Y軸邊框顏色
        },
    });

    // 加入 K 線數據系列
    candleSeries = chart.addCandlestickSeries({
        upColor: '#00ff88',       // 漲：綠色
        downColor: '#ff4d4d',     // 跌：紅色
        borderDownColor: '#ff4d4d',
        borderUpColor: '#00ff88',
        wickDownColor: '#ff4d4d',
        wickUpColor: '#00ff88',
    });

    // 監聽視窗大小改變，自動調整圖表尺寸
    window.addEventListener('resize', () => {
        if(chart && container) {
             chart.resize(container.clientWidth, 400);
        }
    });
}

/**
 * 抓取 K 線資料並更新圖表
 * @param {boolean} shouldResetZoom - 是否需要重置縮放 (預設為 false)
 * 用途：切換週期或幣種時傳入 true，讓圖表自動適配範圍；自動更新時傳入 false，避免畫面一直跳動。
 */
async function fetchCandles(shouldResetZoom = false) {
    // 1. 取得目前選中的幣種，如果沒有就預設用 BTCUSDT
    const symbolId = document.getElementById('tradeSymbol').value || 'BTCUSDT';
    
    // 2. 檢查圖表系列 (candleSeries) 是否已建立，沒建立就無法更新數據，直接返回
    if (!candleSeries) return;

    // 3. 轉換成大寫以符合後端或幣安 API 格式 (例如 btcusdt -> BTCUSDT)
    const binanceSymbol = symbolId.toUpperCase();

    try {
        // 4. 發送 API 請求，帶上幣種與當前的時間週期 (interval)
        // currentChartInterval 是全域變數，例如 '1m', '15m', '1h'
        const res = await fetch(`/api/candles/proxy/${binanceSymbol}?interval=${currentChartInterval}`);
        
        // 5. 判斷回應是否成功 (HTTP 200 OK)
        if (res.ok) {
            const data = await res.json();
            
            // 6. 確保回傳的是陣列且有資料
            if (Array.isArray(data) && data.length > 0) {
                
                // 7. 資料轉換 (Mapping)
                // 幣安 API 回傳格式通常是陣列 [time, open, high, low, close, ...]
                // Lightweight Charts 需要物件格式 { time, open, high, low, close }
                const chartData = data.map(d => ({
                    // d[0] 是毫秒時間戳，除以 1000 轉為秒
                    // + (8 * 3600) 是因為 Lightweight Charts 預設用 UTC，這裡手動加 8 小時轉為 UTC+8 (台灣時間)
                    time: d[0] / 1000 + (8 * 3600), 
                    open: parseFloat(d[1]),  // 開盤價 (字串轉浮點數)
                    high: parseFloat(d[2]),  // 最高價
                    low: parseFloat(d[3]),   // 最低價
                    close: parseFloat(d[4])  // 收盤價
                }));

                // 8. 確保資料按時間排序 (圖表庫要求時間必須遞增)
                chartData.sort((a, b) => a.time - b.time);
                
                const lastPrice = chartData[chartData.length - 1].close;
                let precision = 2;   // 預設 2 位小數 (如 123.45)
                let minMove = 0.01;  // 最小跳動單位

                if (lastPrice < 1) {
                    // 如果價格小於 1 (如 DOGE, SHIB)，需要更多小數位
                    precision = 6;      // 顯示 6 位小數
                    minMove = 0.000001; 
                } else if (lastPrice < 10) {
                    precision = 4;
                    minMove = 0.0001;
                } else if (lastPrice > 1000) {
                    // 如果價格很大 (如 BTC)，2 位小數就夠了，甚至可以改 1 位
                    precision = 2;
                    minMove = 0.01;
                }

                // 動態套用設定給 candleSeries
                candleSeries.applyOptions({
                    priceFormat: {
                        type: 'price',
                        precision: precision,
                        minMove: minMove,
                    },
                });
                
                // 9. 將整理好的資料餵給圖表
                candleSeries.setData(chartData);

                // === [修正] 改用 setTimeout 延遲執行 ===
                if (shouldResetZoom) {
                    setOptimalView(); // <--- 這裡原本是 chart.timeScale().fitContent()
                }
                // ======================================

                return;
            }
        }
        // 如果回應不 ok 或資料格式不對，拋出錯誤進入 catch
        throw new Error("Invalid API response");

    } catch (err) {
        // 11. 錯誤處理：印出警告，並改用假資料 (Dummy Data) 顯示
        // 這在開發階段或 API 掛掉時很有用，避免圖表全白
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

        div.innerHTML = `
            <div style="display:flex; justify-content:space-between; margin-bottom:5px;">
                <span style="font-weight:bold; color:${sideColor}">${o.side} ${o.symbolId}</span>
                <span style="font-size:0.8em; color:#aaa;">${o.status}</span>
            </div>
            <div style="font-size:0.9em; display:flex; justify-content:space-between;">
                <span>價格: ${o.price}</span>
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

    // [New] Update Best Bid/Ask Display
    // Best Ask is the lowest sell price (first item in sorted asks array)
    const bestAsk = asks.length > 0 ? asks[0].price : '---';
    // Best Bid is the highest buy price (last item in sorted bids array - actually first item in DESC sorted bids)
    // We used slice(0, 12) from DESC sorted bids to get best bids.
    // bestBids is then sorted ASC for display. So last element is highest.
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

    const content = document.getElementById('historyContent');
    content.innerHTML = '<div style="text-align:center; padding:10px;">載入中...</div>';

    if (tab === 'FUNDS') fetchHistoryFunds();
    else if (tab === 'TRADES') fetchHistoryTrades();
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

function resetChartZoom() {
    if (chart) {
        setOptimalView();
    }
}

function setOptimalView() {
    if (!chart || !candleSeries) return;

    const data = candleSeries.data();
    if (data.length === 0) return;

    // 設定顯示最近 80 根
    const visibleCandles = 80;
    const totalLength = data.length;
    const fromIndex = totalLength - visibleCandles;
    const toIndex = totalLength + 5; 

    setTimeout(() => {
        // 1. 設定 X 軸 (時間) 範圍
        chart.timeScale().setVisibleLogicalRange({
            from: fromIndex,
            to: toIndex
        });

        // 2. [新增] 強制 Y 軸 (價格) 根據目前的可見範圍進行 "Auto Scale"
        // 這會忽略掉畫面以外的極端價格，讓當前的 K 線胖瘦適中
        chart.priceScale('right').applyOptions({
            autoScale: true, // 開啟自動縮放
            scaleMargins: {  // 設定上下邊界留白，讓 K 線不要頂天立地
                top: 0.1,    // 上面留 10% 空白
                bottom: 0.1, // 下面留 10% 空白
            },
        });

        chart.timeScale().scrollToPosition(5, true); 
    }, 10);
}