// ==========================================================================
// 核心 DOM 節點追蹤
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");
const gameViewport = document.getElementById("gameViewport");

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
// 3按鈕強效解析生成引擎
// ==========================================================================
function parsingChoiceOptions(fullText) {
    choiceContainer.innerHTML = ""; // 倒進去前先清空容器
    const optionLines = fullText.split('\n');
    let count = 0;
    
    optionLines.forEach(line => {
        const trimmedLine = line.trim();
        
        // 如果抓取到符合 1., 2., 3. 或 A., B., C. 開頭的行，就將其剝離並封裝成按鈕
        if (trimmedLine.match(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/) && count < 3) {
            count++;
            const btn = document.createElement("div");
            btn.classList.add("game-choice-btn");
            
            // 去除行首的數字編號標記，僅留下乾淨的動作/語句文本
            const cleanButtonText = trimmedLine.replace(/^([1-3A-Ca-c一二三]{1}[\.、\s\-\:\)]|\[選項[1-3]\])/, "").trim();
            btn.innerText = cleanButtonText;
            
            // 綁定點擊事件：點擊即清空按鈕並向 AI 發送決定
            btn.onclick = () => {
                choiceContainer.innerHTML = ""; 
                processGameAction(`【玩家選擇了以下行動】：${cleanButtonText}`);
            };
            choiceContainer.appendChild(btn);
        }
    });
}

// ==========================================================================
// AI 核心通訊與異步流處理
// ==========================================================================
async function processGameAction(actionPayloadText) {
    speakerTag.innerText = "因果演算中...";
    dialogueText.innerText = "正在推演時空分歧線，抉擇即刻呈現...";
    choiceContainer.innerHTML = "";

    let runningResponse = "";

    try {
        // 調用 Puter 全域 AI 介面連線 Gemini
        const responseStream = await puter.ai.chat(actionPayloadText, {
            model: 'gemini-3.1-pro-preview',
            stream: true,
            instructions: `你是一款現代高中校園戀愛養成 Galgame 的文本核心系統。
請嚴格依照設定與玩家進行互動：

【核心規則】
1. 每一回合請提供極具動漫分鏡感、筆觸細膩且富有情感波折的簡短「場景與心理描述」。
2. 你必須自行生成三個截然不同的行為動作或說話語句選項供玩家選擇。
3. 為利解析引擎處理，你必須在輸出文本的【最後三行】，嚴格使用以下數字序號開頭格式輸出選項內容，不可夾帶任何其他符號或說明：
1. 第一個行為或對話選項
2. 第二個行為或對話選項
3. 第三個行為或對話選項`
        });

        // 正確的 JavaScript 流式文字接收循環語法
        for await (const part of responseStream) {
            if (part?.text) {
                runningResponse += part.text;
                updateDialogueUI(runningResponse);
            }
        }
        
        // 渲染 3 個互動選項按鈕到畫面中央
        parsingChoiceOptions(runningResponse);

    } catch (error) {
        console.error("Game System Error Trace:", error);
        speakerTag.innerText = "系統錯誤";
        dialogueText.innerText = "命運線連線中斷。請點擊重試。";
    }
}

// ==========================================================================
// 初始主選單按鈕事件綁定
// ==========================================================================
const fallbackStartBtn = document.getElementById("fallbackStartBtn");
if (fallbackStartBtn) {
    fallbackStartBtn.onclick = () => {
        choiceContainer.innerHTML = ""; // 移除開始按鈕
        // 正式喚醒 AI 模型建構第一幕
        processGameAction("【系統啟動】請拉開序幕，以極具畫面感的文筆描述開學第一天早晨的櫻花校園走廊，並引導出跟傲嬌青梅竹馬相遇的初始 3 個行動選項。");
    };
}
