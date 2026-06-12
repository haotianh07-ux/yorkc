// ==========================================================================
// DOM
// ==========================================================================
const dialogueText = document.getElementById("dialogueText");
const speakerTag = document.getElementById("speakerTag");
const choiceContainer = document.getElementById("choiceContainer");

// ==========================================================================
// HUGGING FACE API KEY
// ==========================================================================
const API_KEY = "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";

// Model (you can change later)
const MODEL = "mistralai/Mistral-7B-Instruct-v0.2";

// ==========================================================================
// START GAME
// ==========================================================================
function initGameFlow() {
    choiceContainer.innerHTML = "";
    processGameAction("開學第一天，櫻花校園走廊，與青梅竹馬相遇，開始校園戀愛劇情並生成三個選項。");
}
window.initGameFlow = initGameFlow;

// ==========================================================================
// UI CLEAN
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
// HUGGING FACE API CALL
// ==========================================================================
async function processGameAction(input) {
    speakerTag.innerText = "AI 思考中...";
    dialogueText.innerText = "生成劇情中...";
    choiceContainer.innerHTML = "";

    const systemPrompt = `
你是一個校園戀愛Visual Novel引擎。

規則：
- 描述劇情（有畫面感、簡短）
- 生成3個選項
- 最後三行必須是：
1. xxx
2. xxx
3. xxx
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
                    inputs: systemPrompt + "\n\n玩家輸入：" + input,
                    parameters: {
                        max_new_tokens: 400,
                        temperature: 0.9,
                        return_full_text: false
                    }
                })
            }
        );

        const data = await res.json();
        console.log("HF:", data);

        // Hugging Face sometimes returns array or error object
        let text =
            data?.[0]?.generated_text ||
            data?.generated_text ||
            data?.error;

        if (!text) throw new Error("No response from Hugging Face");

        updateDialogueUI(text);
        parsingChoiceOptions(text);

    } catch (err) {
        console.error(err);

        speakerTag.innerText = "錯誤";
        dialogueText.innerText =
            "連線失敗：\n- Token錯\n- 模型載入中\n- HF限制";

        choiceContainer.innerHTML = "";

        const btn = document.createElement("div");
        btn.className = "game-choice-btn";
        btn.innerText = "重試";
        btn.onclick = initGameFlow;

        choiceContainer.appendChild(btn);
    }
}
