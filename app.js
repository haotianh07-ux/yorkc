// ==========================================================================
// DOM
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");

// ==========================================================================
// GROQ API KEY (PUT YOUR KEY HERE)
// ==========================================================================
const API_KEY = "PUT_YOUR_GROQ_API_KEY_HERE";

// ==========================================================================
// START GAME
// ==========================================================================
function initGameFlow() {
    choiceContainer.innerHTML = "";
    processGameAction("開學第一天，櫻花飄落的校園走廊，與青梅竹馬相遇，開始劇情並生成三個選項。");
}
window.initGameFlow = initGameFlow;

// ==========================================================================
// UI CLEANER
// ==========================================================================
function updateDialogueUI(text) {
    let clean = text;

    const speaker = clean.match(/^([^：:\n]+)[：:]/);
    if (speaker) {
        speakerTag.innerText = speaker[1];
        clean = clean.replace(speaker[0], "");
    } else {
        speakerTag.innerText = "旁白";
    }

    clean = clean.replace(/^\s*[1-3]\..*$/gm, "");
    dialogueText.innerText = clean.trim();
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

            const option = t.replace(/^[1-3]\.\s*/, "");
            btn.innerText = option;

            btn.onclick = () => {
                choiceContainer.innerHTML = "";
                processGameAction("玩家選擇：" + option);
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
// GROQ API CALL
// ==========================================================================
async function processGameAction(input) {
    speakerTag.innerText = "AI 思考中...";
    dialogueText.innerText = "生成劇情中...";
    choiceContainer.innerHTML = "";

    const systemPrompt = `
你是一個校園戀愛Visual Novel引擎。

規則：
- 描述劇情（有畫面感、短）
- 產生3個選項
- 最後三行必須是：
1. xxx
2. xxx
3. xxx
`;

    try {
        const res = await fetch("https://api.groq.com/openai/v1/chat/completions", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${API_KEY}`
            },
            body: JSON.stringify({
                model: "llama-3.1-8b-instant",
                messages: [
                    {
                        role: "system",
                        content: systemPrompt
                    },
                    {
                        role: "user",
                        content: input
                    }
                ],
                temperature: 1,
                max_tokens: 600
            })
        });

        const data = await res.json();
        console.log("GROQ:", data);

        if (!res.ok) {
            throw new Error(data.error?.message || "Groq API error");
        }

        const text = data?.choices?.[0]?.message?.content;

        if (!text) throw new Error("No response from Groq");

        updateDialogueUI(text);
        parsingChoiceOptions(text);

    } catch (err) {
        console.error(err);

        speakerTag.innerText = "錯誤";
        dialogueText.innerText =
            "連線失敗：\n- API key錯\n- 網路問題\n- Groq限制";

        choiceContainer.innerHTML = "";

        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "重試";
        btn.onclick = initGameFlow;

        choiceContainer.appendChild(btn);
    }
}
