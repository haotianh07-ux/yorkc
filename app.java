// ==========================================================================
// 核心 DOM 節點追蹤
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");
const gameViewport = document.getElementById("gameViewport");

// ==========================================================================
// API 金鑰管理庫 (保護金鑰不外洩至 GitHub)
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

// 清除金鑰的快捷功能（如果需要更換金鑰可以在主控台輸入此功能）
function resetApiKey() {
    localStorage.removeItem("gemini_api_key");
    alert("API Key 已清除，請重整網頁重新輸入。");
}

// ==========================================================================
// UI 文字排版與清洗引擎
// ==========================================================================
function updateDialogueUI(rawText) {
    let cleanText = rawText;

    // 1. 處理角色名字標籤（例如 "林曉婷："）
    const speakerMatch = cleanText.match(/^([^：:\n]+)[：:]/);
    if (speakerMatch) {
        speakerTag.innerText = speakerMatch[1].trim();
        cleanText = cleanText.substring(speakerMatch[0].length);
    } else if (!cleanText.startsWith(" ") && cleanText.length > 1) {
        speakerTag.innerText = "旁白描述";
    }

    // 2. 自動過濾隱藏行首帶有選項數字的行，避免顯示在底部對話框內
    cleanText = cleanText.replace(/^\s*([1-3一二三A-Ca-c\-\*•]|選項)[\.、\s\-\:\)].*$/gm, '');

    dialogueText.innerText = cleanText.trim();
}

// ==========================================================================
// 3 按鈕強效解析生成引擎
// ==========================================================================
function parsingChoiceOptions(fullText) {
    choiceContainer.innerHTML = ""; 
    const optionLines = fullText.split('\n');
    let count = 0;
    
    optionLines.forEach(line => {
        const trimmedLine = line.trim();
        
        if (trimmedLine.match(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/) && count < 3) {
            count++;
            const btn = document.createElement("div");
            btn.classList.add("game-choice-btn");
            
            const cleanButtonText = trimmedLine.replace(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/, "").trim();
            btn.innerText = cleanButtonText;
            
            btn.onclick = () => {
                choiceContainer.innerHTML = ""; 
                processGameAction(`【玩家選擇了以下行動】：${cleanButtonText}`);
            };
            choiceContainer.appendChild(btn);
        }
    });
}

// ==========================================================================
// Google Gemini 原生通訊核心 (使用官方 REST Fetch 串流)
// ==========================================================================
async function processGameAction(actionPayloadText) {
    speakerTag.innerText = "因果演算中...";
    dialogueText.innerText = "正在推演時空分歧線，抉擇即刻呈現...";
    choiceContainer.innerHTML = "";

    const apiKey = getApiKey();
    if (!apiKey) {
        speakerTag.innerText = "系統錯誤";
        dialogueText.innerText = "未提供 API 金鑰，無法啟動遊戲。請重整網頁並輸入有效的 Gemini API Key。";
        createResetButton();
        return;
    }

    let runningResponse = "";

    try {
        // 使用 Google 官方當前穩定的 gemini-1.5-flash 模型端點進行實時串流
        const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:streamGenerateContent?key=${apiKey}`;
        
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

        // 讀取 Fetch 回傳的 ReadableStream 資料流
        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;
            
            buffer += decoder.decode(value, { stream: true });
            
            // SSE (Server-Sent Events) 資料流解析處理
            // Gemini 串流會以 [ {"beff":...}, ... ] 的 JSON 陣列塊形式傳回
            let boundary = buffer.indexOf('\n');
            while (boundary !== -1) {
                let line = buffer.substring(0, boundary).trim();
                buffer = buffer.substring(boundary + 1);
                
                // 移除 Gemini 串流特有的逗號與陣列中括號標記
                if (line.startsWith(',')) line = line.substring(1).trim();
                if (line.startsWith('[') || line.startsWith(']')) line = "";
                
                if (line) {
                    try {
                        const parsed = JSON.parse(line);
                        const textChunk = parsed.candidates[0].content.parts[0].text;
                        if (textChunk) {
                            runningResponse += textChunk;
                            updateDialogueUI(runningResponse);
                        }
                    } catch (e) {
                        // 忽略尚未傳輸完整的 JSON 碎片
                    }
                }
                boundary = buffer.indexOf('\n');
            }
        }

        // 結尾補漏解析最後剩餘的 Buffer
        if (runningResponse) {
            updateDialogueUI(runningResponse);
            parsingChoiceOptions(runningResponse);
        }

    } catch (error) {
        console.error("Gemini Direct Connection Error:", error);
        speakerTag.innerText = "連線失敗";
        dialogueText.innerText = "無法連接至 Google Gemini 伺服器。請確認您的 API Key 是否正確，或嘗試清除快取。";
        createResetButton();
    }
}

// 建立安全重試與清除金鑰按鈕
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
// 延時安全初始化掛鉤
// ==========================================================================
function initGameMenu() {
    speakerTag.innerText = "遊戲主選單";
    dialogueText.innerText = "歡迎來到校園戀愛養成遊戲。粉色戀愛序幕已備就，請點擊上方按鈕開始啟程。";
    
    const fallbackStartBtn = document.getElementById("fallbackStartBtn");
    if (fallbackStartBtn) {
        fallbackStartBtn.onclick = () => {
            choiceContainer.innerHTML = ""; 
            processGameAction("【系統啟動】請拉開序幕，以極具畫面感的文筆描述開學第一天早晨的櫻花校園走廊，並引導出跟傲嬌青梅竹馬相遇的初始 3 個行動選項。");
        };
    }
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initGameMenu);
} else {
    initGameMenu();
}
