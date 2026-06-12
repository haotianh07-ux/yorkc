// ==========================================================================
// DOM
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");

// ==========================================================================
// PUT YOUR GEMINI KEY HERE
// IMPORTANT: must be real Gemini API key (usually starts with "AIzaSy")
// ==========================================================================
const API_KEY = "AQ.Ab8RN6IrRpcvPRS5tII0qj_FrPrSK3o-92r-LNaSPtxkJfhH5w";

// ==========================================================================
// START GAME (make sure HTML onclick works)
// ==========================================================================
function initGameFlow() {
    choiceContainer.innerHTML = "";
    processGameAction("開學第一天，櫻花飄落的校園走廊，與青梅竹馬第一次對話。請開始劇情並給出三個選項。");
}

window.initGameFlow = initGameFlow; // IMPORTANT FIX (this often breaks your click)

// ==========================================================================
// UI UPDATE
// ==========================================================================
function updateDialogueUI(text) {
    let cleanText = text;

    const speakerMatch = cleanText.match(/^([^：:\n]+)[：:]/);

    if (speakerMatch) {
        speakerTag.innerText = speakerMatch[1];
        cleanText = cleanText.replace(speakerMatch[0], "");
    } else {
        speakerTag.innerText = "旁白";
    }

    // remove option lines from main text
    cleanText = cleanText.replace(/^\s*[1-3]\..*$/gm, "");

    dialogueText.innerText = cleanText.trim();
}

// ==========================================================================
// CHOICE PARSER
// ==========================================================================
function parsingChoiceOptions(text) {
    choiceContainer.innerHTML = "";

    const lines = text.split("\n");
    let count = 0;

    lines.forEach(line => {
        const t = line.trim();

        if (/^[1-3]\./.test(t) && count < 3) {
            count++;

            const btn = document.createElement("div");
            btn.className = "game-choice-btn";

            const optionText = t.replace(/^[1-3]\.\s*/, "");
            btn.innerText = optionText;

            btn.onclick = () => {
                choiceContainer.innerHTML = "";
                processGameAction("玩家選擇：" + optionText);
            };

            choiceContainer.appendChild(btn);
        }
    });

    if (count === 0) {
        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "繼續";
        btn.onclick = () => processGameAction("繼續劇情");
        choiceContainer.appendChild(btn);
    }
}

// ==========================================================================
// GEMINI CALL
// ==========================================================================
async function processGameAction(input) {
    speakerTag.innerText = "AI 思考中...";
    dialogueText.innerText = "正在生成劇情...";
    choiceContainer.innerHTML = "";

    try {
        const res = await fetch(
            `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${API_KEY}`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    contents: [
                        {
                            parts: [
                                {
                                    text: `
你是一個校園戀愛Galgame。

規則：
- 描述劇情（短但有畫面感）
- 提供3個選項
- 最後三行必須是：
1. xxx
2. xxx
3. xxx

玩家輸入：${input}
                                    `
                                }
                            ]
                        }
                    ]
                })
            }
        );

        const data = await res.json();

        console.log("Gemini response:", data);

        if (!res.ok) {
            throw new Error(data.error?.message || "API error");
        }

        const text = data?.candidates?.[0]?.content?.parts?.[0]?.text;

        if (!text) throw new Error("No response text");

        updateDialogueUI(text);
        parsingChoiceOptions(text);

    } catch (err) {
        console.error(err);

        speakerTag.innerText = "錯誤";
        dialogueText.innerText =
            "連線失敗：\n1. API key錯\n2. 網路問題\n3. API未啟用";

        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "重試";
        btn.onclick = initGameFlow;

        choiceContainer.innerHTML = "";
        choiceContainer.appendChild(btn);
    }
}
