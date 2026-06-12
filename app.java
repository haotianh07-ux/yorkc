// ==========================================================================
// DOM Elements
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");

// ==========================================================================
// API Key Management
// ==========================================================================
function getApiKey() {
    let apiKey = localStorage.getItem("gemini_api_key");

    if (!apiKey) {
        apiKey = prompt("🌸 請輸入 Gemini API Key：");
        if (apiKey) {
            apiKey = apiKey.trim();
            localStorage.setItem("gemini_api_key", apiKey);
        }
    }

    return apiKey;
}

function resetApiKey() {
    localStorage.removeItem("gemini_api_key");
    alert("API Key 已清除，請重新整理頁面。");
}

// ==========================================================================
// Dialogue UI
// ==========================================================================
function updateDialogueUI(rawText) {
    let cleanText = rawText;

    const speakerMatch = cleanText.match(/^([^：:\n]+)[：:]/);

    if (speakerMatch) {
        speakerTag.innerText = speakerMatch[1].trim();
        cleanText = cleanText.substring(speakerMatch[0].length);
    } else {
        speakerTag.innerText = "旁白";
    }

    cleanText = cleanText.replace(
        /^\s*(?:[1-3]|[ABCabc]|[一二三]|選項)[\.、\s\-:\)].*$/gm,
        ""
    );

    dialogueText.innerText = cleanText.trim();
}

// ==========================================================================
// Choice Parsing
// ==========================================================================
function parsingChoiceOptions(fullText) {
    choiceContainer.innerHTML = "";
    const lines = fullText.split("\n");

    let count = 0;

    lines.forEach(line => {
        const trimmed = line.trim();

        if (/^(?:[1-3])\./.test(trimmed) && count < 3) {
            count++;

            const btn = document.createElement("div");
            btn.classList.add("game-choice-btn");

            const text = trimmed.replace(/^(?:[1-3])\.\s*/, "");
            btn.innerText = text;

            btn.onclick = () => {
                choiceContainer.innerHTML = "";
                processGameAction(`玩家選擇：${text}`);
            };

            choiceContainer.appendChild(btn);
        }
    });

    if (count === 0) {
        const retry = document.createElement("div");
        retry.classList.add("game-choice-btn");
        retry.innerText = "繼續";
        retry.onclick = () => processGameAction("繼續劇情");
        choiceContainer.appendChild(retry);
    }
}

// ==========================================================================
// Gemini API
// ==========================================================================
async function processGameAction(actionPayloadText) {
    speakerTag.innerText = "系統";
    dialogueText.innerText = "AI 思考中...";
    choiceContainer.innerHTML = "";

    const apiKey = getApiKey();

    if (!apiKey) {
        dialogueText.innerText = "未提供 API key";
        createResetButton();
        return;
    }

    const systemPrompt = `
你是一款校園戀愛養成 Galgame 的文本引擎。

規則：
1. 每回合先描述場景與情緒
2. 產生三個選項
3. 最後三行必須是：

1. 選項一
2. 選項二
3. 選項三
`;

    try {
        const response = await fetch(
            `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    contents: [
                        {
                            role: "user",
                            parts: [
                                {
                                    text:
                                        systemPrompt +
                                        "\n\n玩家輸入：" +
                                        actionPayloadText
                                }
                            ]
                        }
                    ]
                })
            }
        );

        const data = await response.json();
        console.log(data);

        if (!response.ok) {
            throw new Error(
                data.error?.message || `HTTP Error ${response.status}`
            );
        }

        const responseText =
            data?.candidates?.[0]?.content?.parts?.[0]?.text;

        if (!responseText) {
            throw new Error("Gemini 沒有回傳內容");
        }

        updateDialogueUI(responseText);
        parsingChoiceOptions(responseText);

    } catch (error) {
        console.error(error);
        speakerTag.innerText = "錯誤";
        dialogueText.innerText =
            "連線失敗。\n可能原因：\n1. API key 錯誤\n2. CORS\n3. Gemini quota 用完";
        createResetButton();
    }
}

// ==========================================================================
// Retry Buttons
// ==========================================================================
function createResetButton() {
    choiceContainer.innerHTML = "";

    const retryBtn = document.createElement("div");
    retryBtn.classList.add("game-choice-btn");
    retryBtn.innerText = "🔄 重試";
    retryBtn.onclick = () => initGameFlow();

    const resetBtn = document.createElement("div");
    resetBtn.classList.add("game-choice-btn");
    resetBtn.style.marginTop = "10px";
    resetBtn.innerText = "🔑 更換 API Key";
    resetBtn.onclick = resetApiKey;

    choiceContainer.appendChild(retryBtn);
    choiceContainer.appendChild(resetBtn);
}

// ==========================================================================
// Start Game
// ==========================================================================
function initGameFlow() {
    choiceContainer.innerHTML = "";

    processGameAction(`
開學第一天早晨。
櫻花校園走廊。
主角與傲嬌青梅竹馬相遇。
開始故事。
`);
}

// Make sure HTML onclick can access it
window.initGameFlow = initGameFlow;
