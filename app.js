// ==========================
// DOM
// ==========================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");

// ==========================
// HUGGING FACE CONFIG
// ==========================
const API_KEY = "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";
const MODEL = "mistralai/Mistral-7B-Instruct-v0.2";

// ==========================
// START GAME
// ==========================
function initGameFlow() {
    choiceContainer.innerHTML = "";
    processGameAction("開學第一天，櫻花飄落的校園走廊，與青梅竹馬相遇。生成劇情與3個選項。");
}
window.initGameFlow = initGameFlow;

// ==========================
// UI CLEANER
// ==========================
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

// ==========================
// CHOICE PARSER
// ==========================
function parsingChoiceOptions(text) {
    choiceContainer.innerHTML = "";

    const lines = text.split("\n");
    let count = 0;

    for (const line of lines) {
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
    }

    if (count === 0) {
        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "繼續";
        btn.onclick = () => processGameAction("繼續劇情");
        choiceContainer.appendChild(btn);
    }
}

// ==========================
// HUGGING FACE CALL (FIXED)
// ==========================
async function processGameAction(input) {
    speakerTag.innerText = "AI 思考中...";
    dialogueText.innerText = "生成劇情中...";
    choiceContainer.innerHTML = "";

    const prompt = `
你是一個校園戀愛Visual Novel引擎。

規則：
1. 描述短劇情
2. 必須提供3個選項
3. 最後3行格式必須是：
1. xxx
2. xxx
3. xxx

玩家輸入：${input}
`;

    try {
        const res = await fetch(
            `https://api-inference.huggingface.co/models/${MODEL}`,
            {
                method: "POST",
                headers: {
                    "Authorization": `Bearer ${API_KEY}`,
                    "Content-Type": "application/json"
                },
                body: JSON.stringify({
                    inputs: prompt,
                    parameters: {
                        max_new_tokens: 300,
                        temperature: 0.9,
                        return_full_text: false
                    }
                })
            }
        );

        const data = await res.json();
        console.log("HF RAW:", data);

        // ==========================
        // ERROR HANDLING FIX
        // ==========================
        if (data.error) {
            throw new Error(data.error);
        }

        let text = "";

        if (Array.isArray(data)) {
            text = data[0]?.generated_text;
        } else {
            text = data.generated_text;
        }

        if (!text) {
            throw new Error("Empty response from model");
        }

        updateDialogueUI(text);
        parsingChoiceOptions(text);

    } catch (err) {
        console.error("ERROR:", err);

        speakerTag.innerText = "系統錯誤";
        dialogueText.innerText =
            "AI 連線失敗：\n- 模型載入中（常見）\n- Token錯誤\n- HF限制";

        choiceContainer.innerHTML = "";

        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "重試";
        btn.onclick = initGameFlow;

        choiceContainer.appendChild(btn);
    }
}
