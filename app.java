// ==========================================================================
// 核心 DOM 節點追蹤
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");
const gameViewport = document.getElementById("gameViewport");

// ==========================================================================
// API 金鑰管理庫 (保護金鑰，只存於個人瀏覽器 localStorage)
// ==========================================================================
function getApiKey() {
    let apiKey = localStorage.getItem("gemini_api_key");
    if (!apiKey) {
        apiKey = prompt("🌸 請輸入您的 Gemini API Key 以啟動遊戲：\n(金鑰將安全儲存在您的瀏覽器中，不會上傳至 GitHub)");
        if (apiKey) {
            localStorage.setItem("gemini_api_key", apiKey.trim());
        }
    }
    return apiKey;
}

function resetApiKey() {
    localStorage.removeItem("gemini_api_key");
    alert("API Key 已清除，請重整網頁重新輸入。");
}

// ==========================================================================
// UI 文字排版與清洗引擎 (過濾對話框雜音)
// ==========================================================================
function updateDialogueUI(rawText) {
    let cleanText = rawText;

    // 1. 處理角色名字標籤（例如 "旁白：" 或 "青梅竹馬："）
    const speakerMatch = cleanText.match(/^([^：:\n]+)[：:]/);
    if (speakerMatch) {
        speakerTag.innerText = speakerMatch[1].trim();
        cleanText = cleanText.substring(speakerMatch[0].length);
    } else if (!cleanText.startsWith(" ") && cleanText.length > 1) {
        speakerTag.innerText = "旁白描述";
    }

    // 2. 自動過濾隱藏行首帶有選項數字的行，避免出現在底部對話框
    cleanText = cleanText.replace(/^\s*([1-3一二三A-Ca-c\-\*•]|選項)[\.、\s\-\:\)].*$/gm, '');

    dialogueText.innerText = cleanText.trim();
}

// ==========================================================================
// 3 按鈕強效解析生成引擎 (將 AI 的文字選項轉為粉色膠囊按鈕)
// ==========================================================================
function parsingChoiceOptions(fullText) {
    choiceContainer.innerHTML = ""; // 生成前先清空中央容器
    const optionLines = fullText.split('\n');
    let count = 0;
    
    optionLines.forEach(line => {
        const trimmedLine = line.trim();
        
        // 精準捕捉符合 1., 2., 3. 開頭的行
        if (trimmedLine.match(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/) && count < 3) {
            count++;
            const btn = document.createElement("div");
            btn.classList.add("game-choice-btn");
            
            // 剝離行首的數字與點號，保留純淨的動作或對話內容
            const cleanButtonText = trimmedLine.replace(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/, "").trim();
            btn.innerText = cleanButtonText;
            
            // 點擊選項按鈕：清空畫面並將玩家決定傳遞回 AI
            btn.onclick = () => {
                choiceContainer.innerHTML = ""; 
                processGameAction(`【玩家選擇了以下行動】：${cleanButtonText}`);
            };
            choiceContainer.appendChild(btn);
        }
    });
}

// ==========================================================================
// Google Gemini 標準通訊核心 (常規直連 Fetch 版本)
// ==========================================================================
async function processGameAction(actionPayloadText) {
    speakerTag.innerText = "因果演算中...";
    dialogueText.innerText = "正在推演時空分歧線，抉擇即刻呈現...";
    choiceContainer.innerHTML = "";

    // 呼叫金鑰彈窗
    const apiKey = getApiKey();
    if (!apiKey) {
        speakerTag.innerText = "系統提示";
        dialogueText.innerText = "未提供 API 金鑰，請點擊下方按鈕重新輸入以開啟遊戲。";
        createResetButton();
        return;
    }

    try {
        // 使用極為穩定的標準 generateContent 終端點
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
        
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                contents: [{ parts: [{ text: actionPayloadText }] }],
                systemInstruction: {
                    parts: [{ text: `你是一款現代高中校園戀愛養成 Galgame 的文本核心系統。
請嚴格依照設定與玩家進行互動：

【核心規則】
1. 每一回合請提供極具動漫分鏡感、筆觸細膩且富有情感波折的簡短「場景與心理描述」。
2. 你必須自行生成三個截然不同的行為動作或說話語句選項供玩家選擇。
3. 為利解析引擎處理，你必須在輸出文本的【最後三行】，嚴格使用以下數字序號開頭格式輸出選項內容，不可夾帶任何其他符號或說明：
1. 第一個行為或對話選項
2. 第二個行為或對話選項
3. 第三個行為或對話選項` }]
                }
            })
        });

        if (!response.ok) {
            throw new Error(`HTTP 錯誤！狀態碼: ${response.status}`);
        }

        const data = await response.json();
        const responseText = data.candidates[0].content.parts[0].text;

        if (responseText) {
            // 更新底部劇情描述
            updateDialogueUI(responseText);
            // 在畫面中央生成 3 個行為按鈕
            parsingChoiceOptions(responseText);
        } else {
            throw new Error("模型回傳文本為空");
        }

    } catch (error) {
        console.error("Gemini Connection Error:", error);
        speakerTag.innerText = "連線失敗";
        dialogueText.innerText = "無法連接至 Google Gemini。這可能是因為金鑰輸入錯誤，或是您的網路封鎖了 Google API。";
        createResetButton();
    }
}

// ==========================================================================
// 錯誤安全防護：建立重試與更換金鑰按鈕
// ==========================================================================
function createResetButton() {
    choiceContainer.innerHTML = "";
    
    const retryBtn = document.createElement("div");
    retryBtn.classList.add("game-choice-btn");
    retryBtn.innerText = "🔄 重試目前關卡 🔄";
    retryBtn.onclick = () => {
        processGameAction("【系統啟動】請拉開序幕，以極具畫面感的文筆描述開學第一天早晨的櫻花校園走廊，並引導出跟傲嬌青梅竹馬相遇的初始 3 個行動選項。");
    };
    
    const clearKeyBtn = document.createElement("div");
    clearKeyBtn.classList.add("game-choice-btn");
    clearKeyBtn.style.marginTop = "10px";
    clearKeyBtn.innerText = "🔑 更換 API 金鑰 🔑";
    clearKeyBtn.onclick = () => {
        resetApiKey();
    };

    choiceContainer.appendChild(retryBtn);
    choiceContainer.appendChild(clearKeyBtn);
}

// ==========================================================================
// HTML 按鈕點擊直接觸發點 (與全新 index.html 的 onclick 對接)
// ==========================================================================
function initGameFlow() {
    // 立即移除 HTML 內建的開始按鈕
    choiceContainer.innerHTML = ""; 
    // 正式啟動 AI 連線與金鑰驗證
    processGameAction("【系統啟動】請拉開序幕，以極具畫面感的文筆描述開學第一天早晨的櫻花校園走廊，並引導出跟傲嬌青梅竹馬相遇的初始 3 個行動選項。");
}
