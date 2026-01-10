// ====== 檔案總結 ======
// script.js 是星際交易所前端的核心邏輯控制器。
// 架構模式：原生 JS (Vanilla JS) 實作的 SPA (Single Page Application)。
// 主要模組：
// 1. 導航系統 (Navigation)：控制 Panel 切換。
// 2. 認證系統 (Auth)：登入、註冊、Session 檢查。
// 3. 錢包系統 (Wallet)：資產列表渲染、儲值操作。
// 4. 交易系統 (Trade)：K線圖表(Lightweight Charts)、訂單簿、下單、撤單、歷史紀錄。

// ====== 全域常數與設定 ======
const API_URL = '/api/members';
const WALLET_API_URL = '/api/wallets';
const ORDER_API_URL = '/api/orders';

// 支援的幣種列表，將由後端 API 動態載入
let SUPPORTED_COINS = []; 

// ====== 全域狀態變數 (State Management) ======
let orderBookInterval = null; // 訂單簿輪詢計時器
let currentTradeType = 'SPOT'; // 當前交易模式 (SPOT/CONTRACT)
let currentOrderSide = 'BUY'; // 當前下單方向
let currentHistoryTab = 'FUNDS'; // 歷史紀錄分頁
let currentOrderFilter = 'ALL'; // 訂單篩選器
let currentChartInterval = '1m'; // 當前 K 線週期
let allMyOrders = []; // 快取我的訂單列表

// ====== 初始化邏輯 (Initialization) ======
window.onload = async () => {
    // 1. 初始化下拉選單事件
    setupDropdown(); 
    // 2. 載入系統支援幣種
    await fetchSupportedCoins(); 
    
    // 3. 檢查登入狀態 (Session Check)
    try {
        const res = await fetch(`${API_URL}/me`);
        if(res.ok) {
            // 已登入 -> 顯示儀表板
            showDashboard(); 
        } else {
            // 未登入 -> 顯示登入畫面
            showLogin(); 
        }
    } catch (e) {
        console.error("Session check failed", e);
        showLogin();
    }
};

// 輔助函式：從後端獲取幣種列表
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

// ====== 導航系統 (Navigation System) ======

// 隱藏所有面板，並清理狀態 (如計時器、錯誤訊息)
function hideAllPanels() {
    const panels = ['loginPanel', 'registerPanel', 'dashboardPanel', 'profilePanel', 'walletPanel', 'tradePanel'];
    panels.forEach(id => {
        const el = document.getElementById(id);
        if (el) el.classList.add('hidden');
    });
    closeDepositModal();
    clearMsgs();

    // 停止訂單簿輪詢，節省資源
    if (orderBookInterval) {
        clearInterval(orderBookInterval);
        orderBookInterval = null;
    }
}

// 顯示特定面板
function showPanel(panelId) {
    hideAllPanels();
    const el = document.getElementById(panelId);
    if(el) el.classList.remove('hidden');
}

// 各頁面切換函式
function showLogin() { showPanel('loginPanel'); }
function showRegister() { showPanel('registerPanel'); }

function showDashboard() {
    showPanel('dashboardPanel');
    fetchSimpleProfile(); // 載入歡迎訊息
}

function showProfile() {
    showPanel('profilePanel');
    fetchProfileDetails(); // 載入詳細個資
}

function showWallet() {
    showPanel('walletPanel');
    renderWallets(); // 渲染資產列表
}

// 進入交易中心
function showSpot() {
    showPanel('tradePanel');
    document.getElementById('tradePanelTitle').innerText = '交易中心 (Trading Center)';
    const posSection = document.getElementById('positionSection');
    if(posSection) posSection.style.display = 'none'; // 現貨模式隱藏倉位區塊
    setTradeType('SPOT'); 
    switchHistoryTab('FUNDS');

    // [修正] 強制圖表重新調整大小 (Resize Chart)
    // 由於 div 剛從 hidden 狀態恢復，寬度可能尚未正確計算，需延遲執行
    setTimeout(() => {
        if (chart) {
            const container = document.getElementById('chartContainer');
            chart.resize(container.clientWidth, 400); 
            chart.timeScale().fitContent(); 
        }
    }, 0);
}

// 清除所有錯誤/提示訊息
function clearMsgs() {
    const ids = ['loginMsg', 'regMsg', 'profileMsg'];
    ids.forEach(id => {
        const el = document.getElementById(id);
        if(el) el.innerText = '';
    });
}

// ====== 會員認證 (Authentication) ======

// 登入邏輯
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

// 登出邏輯
async function logout() {
    try {
        await fetch(`${API_URL}/logout`, { method: 'POST' });
        showLogin();
    } catch(err) {
        console.error(err);
        showLogin();
    }
}

// 註冊邏輯
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

// 獲取簡易個人資料 (用於儀表板歡迎詞)
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

// ====== 個人資料 (Profile Management) ======

let currentUserData = null;

// 獲取詳細個人資料
async function fetchProfileDetails() {
    const msg = document.getElementById('profileMsg');
    msg.innerText = '';
    disableEditMode(); // 重置為檢視模式

    try {
        const res = await fetch(`${API_URL}/me`);
        if(res.ok) {
            const data = await res.json();
            currentUserData = data;
            
            // 填入檢視模式欄位
            document.getElementById('displayAccount').innerText = data.account;
            document.getElementById('viewName').innerText = data.name;
            document.getElementById('viewNumber').innerText = data.number;

            // 填入編輯模式欄位
            document.getElementById('updateName').value = data.name;
            document.getElementById('updateNumber').value = data.number;
            document.getElementById('updatePassword').value = ''; // 密碼欄位留空
        } else {
            msg.innerText = '無法載入資料';
        }
    } catch(err) {
        msg.innerText = '連線錯誤';
    }
}

// 切換至編輯模式：顯示輸入框，隱藏純文字
function enableEditMode() {
    const panel = document.getElementById('profilePanel');
    panel.classList.add('editing');
}

// 切換至檢視模式
function disableEditMode() {
    const panel = document.getElementById('profilePanel');
    panel.classList.remove('editing');
}

// 提交個人資料更新
async function updateProfile() {
    const name = document.getElementById('updateName').value;
    const number = document.getElementById('updateNumber').value;
    const pass = document.getElementById('updatePassword').value;
    const msg = document.getElementById('profileMsg');

    const payload = {
        name: name,
        number: number
    };
    // 只有當使用者有輸入密碼時才放入 payload
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

// ====== 錢包管理 (Wallet System) ======

let currentWallets = [];

// 渲染錢包列表
async function renderWallets() {
    const listEl = document.getElementById('walletList');
    listEl.innerHTML = '<div style="color:#aaa;">載入中...</div>';

    try {
        const res = await fetch(WALLET_API_URL);
        if(res.ok) {
            let data = await res.json(); // 取得資料庫中的錢包資料
            
            // 邏輯：合併後端回傳的錢包與所有支援的幣種
            // 目的是確保即使餘額為 0 的幣種也能顯示在列表中
            const walletMap = {};
            data.forEach(w => {
                walletMap[w.coinId] = w;
            });

            let mergedWallets = SUPPORTED_COINS.map(coin => {
                if (walletMap[coin]) {
                    return walletMap[coin];
                } else {
                    // 若無錢包資料，則建立虛擬的 0 餘額物件
                    return { coinId: coin, balance: 0, available: 0 };
                }
            });
            
            currentWallets = mergedWallets; 
            
            // 處理排序與過濾 (Sort & Filter)
            const sortBy = document.getElementById('walletSortBy').value;
            const sortOrder = document.getElementById('walletSortOrder').value;
            const hideZero = document.getElementById('walletHideZero').checked;

            let displayData = [...mergedWallets]; 

            if (hideZero) {
                // 過濾掉餘額為 0 的錢包
                displayData = displayData.filter(w => parseFloat(w.balance) > 0);
            }

            // 執行排序
            displayData.sort((a, b) => {
                let valA = parseFloat(a.balance);
                let valB = parseFloat(b.balance);
                
                if (sortBy === 'NAME') {
                    return sortOrder === 'ASC' ? a.coinId.localeCompare(b.coinId) : b.coinId.localeCompare(a.coinId);
                } 
                // 預設按餘額排序
                return sortOrder === 'ASC' ? valA - valB : valB - valA;
            });

            // 產生 HTML
            listEl.innerHTML = '';
            if(displayData.length === 0) {
                 listEl.innerHTML = '<div style="color:#aaa; width:100%;">無相符資產</div>';
            } else {
                displayData.forEach(w => {
                    const div = document.createElement('div');
                    div.className = 'wallet-card';
                    // 計算凍結金額 (總額 - 可用)
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

// 重置所有資產 (測試用)
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

// --- 儲值 Modal 控制邏輯 ---
function openDepositModal(coinId) {
    document.getElementById('depositModal').classList.remove('hidden');
    document.getElementById('depositCoinName').innerText = coinId;
    document.getElementById('depositAmount').value = '';
}

function closeDepositModal() {
    const el = document.getElementById('depositModal');
    if(el) el.classList.add('hidden');
}

// 提交儲值請求
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


// ====== 交易核心邏輯 (Trading System) ======

let symbolOptions = [];

// 初始化下拉搜尋選單
function setupDropdown() {
    const searchInput = document.getElementById('tradeSymbolSearch');
    const dropdownList = document.getElementById('tradeSymbolList');
    const hiddenInput = document.getElementById('tradeSymbol');

    if(!searchInput || !dropdownList) return;

    // 點擊輸入框時顯示下拉清單
    searchInput.addEventListener('click', (e) => {
        e.stopPropagation();
        renderSymbolDropdown(); 
        dropdownList.classList.remove('hidden');
    });

    // 輸入文字時過濾清單
    searchInput.addEventListener('input', () => {
        renderSymbolDropdown(searchInput.value);
    });

    // 點擊外部時關閉下拉清單
    document.addEventListener('click', (e) => {
        if (!searchInput.contains(e.target) && !dropdownList.contains(e.target)) {
            dropdownList.classList.add('hidden');
        }
    });
}

// 渲染下拉選單項目
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

// 選擇交易對
function selectSymbol(value, text, fetchNow = true) {
    const searchInput = document.getElementById('tradeSymbolSearch');
    const hiddenInput = document.getElementById('tradeSymbol');
    const dropdownList = document.getElementById('tradeSymbolList');

    searchInput.value = value;
    hiddenInput.value = value;
    dropdownList.classList.add('hidden');
    
    // 選中後立即更新數據
    if(fetchNow) {
        fetchMyOrders();
        fetchOrderBook();
        initChart();
        fetchCandles();
    }
}

// 設定交易模式 (現貨/合約) 並載入對應的交易對列表
function setTradeType(type) {
    if (type === 'CONTRACT') {
        console.warn("Contract trading is disabled.");
        type = 'SPOT'; 
    }
    
    currentTradeType = type;
    
    const label = document.getElementById('labelSymbol');
    const hiddenInput = document.getElementById('tradeSymbol'); 
    const currentVal = hiddenInput.value;
    
    // 重建交易對選項 (預設所有非 USDT 幣種配對 USDT)
    symbolOptions = [];
    const coins = SUPPORTED_COINS.filter(c => c !== 'USDT');
    
    if(label) label.innerText = '交易對 (Pair)';
    coins.forEach(coin => {
        symbolOptions.push({
            value: coin + 'USDT',
            text: coin
        });
    });

    // 恢復上次選擇或預設第一個
    const found = symbolOptions.find(o => o.value === currentVal);
    if (found) {
        selectSymbol(found.value, found.text, false);
    } else if (symbolOptions.length > 0) {
        selectSymbol(symbolOptions[0].value, symbolOptions[0].text, false);
    }
    
    // 初始化頁面數據
    fetchMyOrders();
    fetchOrderBook();
    initChart();
    fetchCandles();
    
    // 啟動輪詢機制 (Polling) - 每 2 秒更新一次行情
    console.log("Starting Polling for " + type);
    if (orderBookInterval) clearInterval(orderBookInterval);
    orderBookInterval = setInterval(() => {
        fetchOrderBook();
        fetchCandles();
    }, 2000);
}

// 設定下單方向 (買/賣) 並切換按鈕樣式
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

// 根據訂單類型 (市價/限價) 切換價格輸入框的可用性
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

// 提交訂單
async function submitOrder() {
    const symbolId = document.getElementById('tradeSymbol').value;
    const type = document.getElementById('tradeType').value;
    const quantity = document.getElementById('tradeQuantity').value;
    
    // 限價單才需要價格
    let price = document.getElementById('tradePrice').value;

    // 驗證輸入
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
        // 市價單價格設為 0
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
// 使用 Lightweight Charts 庫
let chart = null;
let candleSeries = null;

// 設定 K 線週期 (1m, 1h, etc.)
function setChartInterval(interval) {
    currentChartInterval = interval;
    
    // 更新按鈕樣式
    const buttons = document.querySelectorAll('#chartTimeframeGroup button');
    
    if (buttons.length > 0) {
        buttons.forEach(btn => {
            // 重置預設樣式
            btn.className = 'btn btn-sm btn-secondary'; 
            btn.style.background = 'transparent';
            btn.style.color = 'var(--star-white)';
            
            // 設定激活樣式
            if (btn.textContent.trim() === interval) {
                btn.className = 'btn btn-sm';
                btn.style.background = 'var(--neon-blue)';
                btn.style.color = 'black';
            }
        });
    }
    
    fetchCandles(true); // 重新載入數據並重置縮放
}

// 初始化圖表
function initChart() {
    const container = document.getElementById('chartContainer');
    if (!container) return;
    
    // 若圖表已存在，先銷毀以防記憶體洩漏
    if (chart) {
        chart.remove();
        chart = null;
    }

    // 建立圖表實例
    chart = LightweightCharts.createChart(container, {
        width: container.clientWidth,
        height: 400,
        // 設定深色主題樣式
        layout: {
            background: { type: 'solid', color: 'transparent' }, // 透明背景
            textColor: '#D9D9D9',
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
        },
    });

    // 加入 K 線系列
    candleSeries = chart.addCandlestickSeries({
        upColor: '#00ff88',       // 漲：綠
        downColor: '#ff4d4d',     // 跌：紅
        borderDownColor: '#ff4d4d',
        borderUpColor: '#00ff88',
        wickDownColor: '#ff4d4d',
        wickUpColor: '#00ff88',
    });

    // 監聽視窗縮放事件，保持響應式
    window.addEventListener('resize', () => {
        if(chart && container) {
             chart.resize(container.clientWidth, 400);
        }
    });
}

/**
 * 抓取 K 線資料並更新圖表
 * @param {boolean} shouldResetZoom - 是否重置縮放 (切換幣種時傳 true)
 */
async function fetchCandles(shouldResetZoom = false) {
    const symbolId = document.getElementById('tradeSymbol').value || 'BTCUSDT';
    
    if (!candleSeries) return;

    const binanceSymbol = symbolId.toUpperCase();

    try {
        // 透過後端 Proxy 請求 Binance 資料
        const res = await fetch(`/api/candles/proxy/${binanceSymbol}?interval=${currentChartInterval}`);
        
        if (res.ok) {
            const data = await res.json();
            
            if (Array.isArray(data) && data.length > 0) {
                
                // 資料格式轉換：Binance Array -> Lightweight Charts Object
                const chartData = data.map(d => ({
                    // Binance 時間為毫秒，需轉為秒。加 8 小時修正為 UTC+8
                    time: d[0] / 1000 + (8 * 3600), 
                    open: parseFloat(d[1]),
                    high: parseFloat(d[2]),
                    low: parseFloat(d[3]),
                    close: parseFloat(d[4])
                }));

                // 按時間排序 (圖表庫要求)
                chartData.sort((a, b) => a.time - b.time);
                
                // 動態設定價格精度 (Precision)
                const lastPrice = chartData[chartData.length - 1].close;
                let precision = 2;
                let minMove = 0.01;

                if (lastPrice < 1) {
                    precision = 6;
                    minMove = 0.000001; 
                } else if (lastPrice < 10) {
                    precision = 4;
                    minMove = 0.0001;
                } else if (lastPrice > 1000) {
                    precision = 2;
                    minMove = 0.01;
                }

                candleSeries.applyOptions({
                    priceFormat: {
                        type: 'price',
                        precision: precision,
                        minMove: minMove,
                    },
                });
                
                // 更新數據
                candleSeries.setData(chartData);

                // 延遲重置視圖，確保渲染完成
                if (shouldResetZoom) {
                    setOptimalView(); 
                }
                return;
            }
        }
        throw new Error("Invalid API response");

    } catch (err) {
        // 若 API 失敗，使用假資料填充，避免圖表空白
        console.warn("Fetch candles failed, using dummy data:", err);
        generateDummyChartData(); 
        if (shouldResetZoom) {
            setOptimalView();
        }
    }
}

// 生成隨機假 K 線資料 (開發測試用)
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


// ====== 訂單列表管理 (Orders) ======

// 獲取我的訂單
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

// 設定訂單篩選器 (全部/未成交/已成交...)
function setOrderFilter(filter) {
    currentOrderFilter = filter;
    
    // 更新按鈕樣式
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

// 渲染訂單列表
function renderOrders() {
    const listEl = document.getElementById('orderList');
    if (!listEl) return;
    listEl.innerHTML = '';

    // 根據交易模式與篩選器過濾訂單
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
        
        // 未成交的訂單顯示撤單按鈕
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

// 撤銷訂單
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
        // 靜默失敗 (Polling 經常發生)
    }
}

// 渲染訂單簿 (深度圖)
function renderOrderBook(data) {
    const asksEl = document.getElementById('orderBookAsks');
    const bidsEl = document.getElementById('orderBookBids');
    if (!asksEl || !bidsEl) return;
    
    asksEl.innerHTML = '';
    bidsEl.innerHTML = '';

    // 賣盤 (Asks): 價格由低到高，顯示前 12 檔
    let asks = [...data.asks].sort((a,b) => a.price - b.price); 
    asks = asks.slice(0, 12); 
    
    // 反轉順序以符合由下而上顯示 (價格低的在最下面，接近中間市價)
    // 注意：CSS flex-direction: column-reverse 已經處理了視覺順序
    asks.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#ff4d4d', 'SELL');
        asksEl.appendChild(div);
    });

    // 買盤 (Bids): 價格由高到低，顯示前 12 檔
    let bids = [...data.bids].sort((a,b) => a.price - b.price); 
    let bestBids = [...data.bids].sort((a,b) => b.price - a.price).slice(0, 12);
    // 再次排序以確保顯示順序 (價格高的在最上面，接近中間市價)
    bestBids.sort((a,b) => b.price - a.price); // 這邊原邏輯可能有誤，通常買盤是價格高的在上面
    
    // 這裡我們維持原有的排序邏輯
    bestBids.sort((a,b) => a.price - b.price); // 重新改回由低到高，讓 DOM 順序一致
    
    bestBids.forEach(e => {
        const div = createBookItem(e.price, e.quantity, '#00f3ff', 'BUY');
        bidsEl.appendChild(div);
    });

    // 更新最佳買賣價顯示
    const bestAsk = asks.length > 0 ? asks[0].price : '---';
    const bestBid = bestBids.length > 0 ? bestBids[bestBids.length - 1].price : '---';

    const askEl = document.getElementById('bestAskPrice');
    if(askEl) askEl.innerText = bestAsk;
    
    const bidEl = document.getElementById('bestBidPrice');
    if(bidEl) bidEl.innerText = bestBid;
}

// 建立訂單簿單列 DOM
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
    
    // 點擊訂單簿可快速帶入價格與方向
    div.onclick = () => {
        document.getElementById('tradePrice').value = price;
        // 點擊賣單 -> 我要買 (BUY)
        // 點擊買單 -> 我要賣 (SELL)
        const targetSide = (itemSide === 'BUY') ? 'SELL' : 'BUY';
        setOrderSide(targetSide);
    };
    return div;
}


// ====== 歷史紀錄 (History) ======

// 切換歷史紀錄分頁 (資金/成交)
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

// 預留方法：獲取合約倉位
function fetchPositions() {
}

// 重置圖表視野
function resetChartZoom() {
    if (chart) {
        setOptimalView();
    }
}

// 計算並設定最佳圖表視野 (只顯示最近 80 根 K 線)
function setOptimalView() {
    if (!chart || !candleSeries) return;

    const data = candleSeries.data();
    if (data.length === 0) return;

    // 設定顯示範圍
    const visibleCandles = 80;
    const totalLength = data.length;
    const fromIndex = totalLength - visibleCandles;
    const toIndex = totalLength + 5; // 右側留白

    setTimeout(() => {
        // 設定 X 軸範圍
        chart.timeScale().setVisibleLogicalRange({
            from: fromIndex,
            to: toIndex
        });

        // 強制 Y 軸自動縮放 (Auto Scale)，忽略畫面外極端值
        chart.priceScale('right').applyOptions({
            autoScale: true, 
            scaleMargins: {  
                top: 0.1,    // 頂部留白
                bottom: 0.1, // 底部留白
            },
        });

        // 稍微往左滾動，確保最新 K 線可見
        chart.timeScale().scrollToPosition(5, true); 
    }, 10);
}
// ====== 備註區 ======
/*
[註1] 輪詢效能 (Polling Performance):
      目前使用 `setInterval` 每 2 秒請求一次 API 來更新訂單簿與 K 線。
      這會對伺服器造成較大負載。
      建議改用 WebSocket (如 STOMP over WebSocket)，實現伺服器主動推播 (Push Notification)，
      僅在資料變動時更新，大幅降低頻寬消耗。

[註2] 錯誤處理 (Error Handling):
      目前的 `try-catch` 區塊多為靜默失敗或簡單印出 console error。
      建議建立統一的 Notification 元件 (如 Toast 訊息)，在 API 失敗時給予用戶友善的提示。
*/