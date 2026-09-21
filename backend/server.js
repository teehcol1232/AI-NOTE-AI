// AI Study Notes Backend Server
// Runs on port 3000, reverse-proxied by Nginx / Cloud Run

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');

// The backend service runs on port 3000 behind Nginx (which listens on 8080)
const PORT = process.env.APP_PORT || process.env.DEFAULT_APP_PORT || 3000;

// Load API keys securely from server-side environment or .dev.env.json
function loadServerKeys() {
    let geminiKey = process.env.GEMINI_API_KEY || '';
    let openAiKey = process.env.OPENAI_API_KEY || '';

    // Check /app/.dev.env.json
    try {
        const devEnvPath = '/app/.dev.env.json';
        if (fs.existsSync(devEnvPath)) {
            const content = JSON.parse(fs.readFileSync(devEnvPath, 'utf8'));
            if (!geminiKey && content.GEMINI_API_KEY) geminiKey = content.GEMINI_API_KEY;
            if (!openAiKey && content.OPENAI_API_KEY) openAiKey = content.OPENAI_API_KEY;
        }
    } catch (e) {
        console.warn('Could not read /app/.dev.env.json:', e.message);
    }

    // Check .env in current directory or parent
    try {
        const envPaths = [path.resolve(process.cwd(), '.env'), '/app/.env', '/app/applet/.env'];
        for (const p of envPaths) {
            if (fs.existsSync(p)) {
                const lines = fs.readFileSync(p, 'utf8').split('\n');
                for (const line of lines) {
                    const trimmed = line.trim();
                    if (trimmed.startsWith('GEMINI_API_KEY=') && !geminiKey) {
                        geminiKey = trimmed.substring('GEMINI_API_KEY='.length).trim();
                    }
                    if (trimmed.startsWith('OPENAI_API_KEY=') && !openAiKey) {
                        openAiKey = trimmed.substring('OPENAI_API_KEY='.length).trim();
                    }
                }
            }
        }
    } catch (e) {
        console.warn('Could not check .env files:', e.message);
    }

    return { geminiKey, openAiKey };
}

const { geminiKey, openAiKey } = loadServerKeys();
console.log(`[AI Study Backend] Keys loaded - Gemini: ${Boolean(geminiKey)}, OpenAI: ${Boolean(openAiKey)}`);

// Helper: Make external HTTPS request
function httpsRequest(urlStr, options, data = null) {
    return new Promise((resolve, reject) => {
        const parsedUrl = new URL(urlStr);
        const reqOptions = {
            hostname: parsedUrl.hostname,
            port: parsedUrl.port || 443,
            path: parsedUrl.pathname + parsedUrl.search,
            method: options.method || 'GET',
            headers: options.headers || {}
        };

        const req = https.request(reqOptions, (res) => {
            let body = '';
            res.on('data', chunk => body += chunk);
            res.on('end', () => {
                resolve({ statusCode: res.statusCode, body, headers: res.headers });
            });
        });

        req.on('error', reject);
        req.setTimeout(45000, () => {
            req.destroy(new Error('Request timeout'));
        });

        if (data) {
            req.write(data);
        }
        req.end();
    });
}

// Call Gemini 3.8 Flash / 3.5 Flash
async function callGeminiApi(prompt, systemInstruction = '', mimeType = 'text/plain') {
    const keys = loadServerKeys();
    const key = keys.geminiKey;
    if (!key) throw new Error('GEMINI_API_KEY is not configured on server');

    const models = ['gemini-3.8-flash', 'gemini-3.5-flash', 'gemini-3.5-flash-lite'];
    let lastError = null;

    for (const model of models) {
        try {
            const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`;
            const payload = {
                contents: [{ parts: [{ text: prompt }] }],
                generationConfig: {
                    responseMimeType: mimeType === 'application/json' ? 'application/json' : 'text/plain',
                    temperature: 0.1
                }
            };
            if (systemInstruction) {
                payload.systemInstruction = { parts: [{ text: systemInstruction }] };
            }

            const res = await httpsRequest(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            }, JSON.stringify(payload));

            if (res.statusCode >= 200 && res.statusCode < 300) {
                const parsed = JSON.parse(res.body);
                const text = parsed?.candidates?.[0]?.content?.parts?.[0]?.text;
                if (text) return text;
            }
            lastError = new Error(`Gemini ${model} returned HTTP ${res.statusCode}: ${res.body}`);
        } catch (err) {
            lastError = err;
        }
    }
    throw lastError || new Error('Gemini API call failed');
}

// Call OpenAI Chat Completions (gpt-4o-mini)
async function callOpenAiApi(prompt, systemInstruction = 'You are an elite educational AI. Always return strictly valid JSON matching the requested schema.', isJson = true) {
    const keys = loadServerKeys();
    const key = keys.openAiKey;
    if (!key) throw new Error('OPENAI_API_KEY is not configured on server');

    const url = 'https://api.openai.com/v1/chat/completions';
    const payload = {
        model: 'gpt-4o-mini',
        messages: [
            { role: 'system', content: systemInstruction },
            { role: 'user', content: prompt }
        ],
        temperature: 0.2
    };
    if (isJson) {
        payload.response_format = { type: 'json_object' };
    }

    const res = await httpsRequest(url, {
        method: 'POST',
        headers: {
            'Authorization': `Bearer ${key}`,
            'Content-Type': 'application/json'
        }
    }, JSON.stringify(payload));

    if (res.statusCode >= 200 && res.statusCode < 300) {
        const parsed = JSON.parse(res.body);
        const text = parsed?.choices?.[0]?.message?.content;
        if (text) return text;
    }
    throw new Error(`OpenAI returned HTTP ${res.statusCode}: ${res.body}`);
}

// Multimodal Gemini vision call
async function callGeminiVision(base64Image, mimeType, prompt) {
    const keys = loadServerKeys();
    const key = keys.geminiKey;
    if (!key) throw new Error('GEMINI_API_KEY is not configured on server');

    const models = ['gemini-3.8-flash', 'gemini-3.5-flash'];
    for (const model of models) {
        try {
            const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${key}`;
            const payload = {
                contents: [{
                    parts: [
                        { inlineData: { mimeType: mimeType || 'image/jpeg', data: base64Image } },
                        { text: prompt }
                    ]
                }],
                generationConfig: {
                    responseMimeType: 'application/json',
                    temperature: 0.1
                }
            };

            const res = await httpsRequest(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }
            }, JSON.stringify(payload));

            if (res.statusCode >= 200 && res.statusCode < 300) {
                const parsed = JSON.parse(res.body);
                const text = parsed?.candidates?.[0]?.content?.parts?.[0]?.text;
                if (text) return text;
            }
        } catch (e) {
            console.warn(`Vision model ${model} error:`, e.message);
        }
    }
    throw new Error('Gemini Vision extraction failed on all models');
}

// Primary AI dispatcher: Tries Gemini first, falls back to OpenAI
async function callAiWithFallback(prompt, systemInstruction = '', isJson = true) {
    const keys = loadServerKeys();

    if (keys.geminiKey) {
        try {
            const res = await callGeminiApi(prompt, systemInstruction, isJson ? 'application/json' : 'text/plain');
            return res;
        } catch (e) {
            console.warn('[AI Study Backend] Gemini failed, trying OpenAI:', e.message);
        }
    }

    if (keys.openAiKey) {
        return await callOpenAiApi(prompt, systemInstruction, isJson);
    }

    throw new Error('No AI provider keys available on server');
}

// Request dispatcher
const server = http.createServer(async (req, res) => {
    // CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Requested-With');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
    const pathname = parsedUrl.pathname;

    console.log(`[${new Date().toISOString()}] ${req.method} ${pathname}`);

    // Helper: Read JSON body
    const readJsonBody = () => new Promise((resolve, reject) => {
        let data = '';
        req.on('data', chunk => {
            data += chunk;
            if (data.length > 25 * 1024 * 1024) { // 25MB limit
                req.destroy(new Error('Payload too large'));
            }
        });
        req.on('end', () => {
            if (!data) return resolve({});
            try {
                resolve(JSON.parse(data));
            } catch (e) {
                reject(new Error('Invalid JSON body'));
            }
        });
        req.on('error', reject);
    });

    const sendJson = (statusCode, data) => {
        res.writeHead(statusCode, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(data));
    };

    try {
        // Health Check
        if (pathname === '/health' || pathname === '/api/health') {
            const keys = loadServerKeys();
            sendJson(200, {
                status: 'ok',
                service: 'AI Study Notes Backend',
                version: '1.0.0',
                timestamp: new Date().toISOString(),
                providers: {
                    geminiConfigured: Boolean(keys.geminiKey),
                    openAiConfigured: Boolean(keys.openAiKey)
                },
                uptime: process.uptime()
            });
            return;
        }

        // Root Dashboard / Status
        if (pathname === '/' || pathname === '/api') {
            const keys = loadServerKeys();
            sendJson(200, {
                name: 'AI Study Notes Backend Service',
                status: 'ONLINE',
                message: 'Backend API is active and ready to process study materials.',
                endpoints: [
                    'GET /health',
                    'POST /api/extract-text',
                    'POST /api/generate-study-package',
                    'POST /api/explain-concept',
                    'POST /api/evaluate-short-answer'
                ],
                providers: {
                    gemini: Boolean(keys.geminiKey) ? 'ACTIVE' : 'NOT_CONFIGURED',
                    openAi: Boolean(keys.openAiKey) ? 'ACTIVE' : 'NOT_CONFIGURED'
                }
            });
            return;
        }

        // Extract Text from Image
        if (pathname === '/api/extract-text' && req.method === 'POST') {
            const body = await readJsonBody();
            const { imageBase64, mimeType, rawText } = body;

            if (rawText && rawText.trim().length > 0) {
                // Text structure refinement
                const prompt = `Analyze this student note:\n"""\n${rawText.trim()}\n"""\nReturn JSON: {"title": "concise title", "subject": "Math/Science/History/etc", "cleanedText": "formatted markdown text", "confidence": 0.95, "legibilityWarning": null}`;
                const resultText = await callAiWithFallback(prompt, 'You are an educational text analyzer. Output strictly valid JSON.', true);
                sendJson(200, JSON.parse(resultText));
                return;
            }

            if (!imageBase64) {
                sendJson(400, { error: 'Either imageBase64 or rawText must be provided' });
                return;
            }

            const prompt = `Transcribe all handwritten and typed text from this image faithfully.\nOutput JSON:\n{\n  "rawText": "full transcript",\n  "cleanedText": "clean formatted markdown",\n  "title": "inferred document title",\n  "subject": "detected subject",\n  "confidence": 0.92,\n  "legibilityWarning": null\n}`;
            const visionResult = await callGeminiVision(imageBase64, mimeType || 'image/jpeg', prompt);
            sendJson(200, JSON.parse(visionResult));
            return;
        }

        // Generate Complete Study Package
        if (pathname === '/api/generate-study-package' && req.method === 'POST') {
            const body = await readJsonBody();
            const { content, topic, difficulty, questionCount = 5, avoidQuestions = [] } = body;

            if (!content || !content.trim()) {
                sendJson(400, { error: 'content is required' });
                return;
            }

            const avoidClause = avoidQuestions.length > 0 
                ? `\nDO NOT repeat or duplicate these previous questions:\n${avoidQuestions.map(q => `- ${q}`).join('\n')}\n`
                : '';

            const prompt = `STRICT GROUNDING MANDATE:
You are an expert tutor creating study materials. Everything you generate MUST be derived STRICTLY from the provided notes text. DO NOT hallucinate external formulas, equations, or concepts that are not present or directly implied in the user's material.
For example, if the text is about Acids and Bases, DO NOT include physics equations like E=mc^2. If the notes have no mathematical formulas, set "formulas": [].

Difficulty: ${difficulty || 'INTERMEDIATE'}
Target Question Count: ${questionCount}
${avoidClause}

USER'S NOTES:
"""
${content.trim()}
"""

Generate a JSON response matching this EXACT schema:
{
  "summary": "Thorough 2-3 paragraph academic summary directly explaining the student notes",
  "keyPoints": ["bullet point 1", "bullet point 2", "bullet point 3"],
  "definitions": [
    {"term": "Term Name", "definition": "Direct definition from notes"}
  ],
  "formulas": [
    {"formula": "actual equation from notes only", "meaning": "what it represents", "variables": ["var: explanation"]}
  ],
  "flashcards": [
    {"front": "Question or prompt?", "back": "Accurate answer grounded in notes"}
  ],
  "questions": [
    {
      "id": "q_1",
      "type": "MULTIPLE_CHOICE",
      "question": "Question text grounded in notes?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctOptionIndex": 0,
      "explanation": "Why this option is correct based on the notes."
    },
    {
      "id": "q_2",
      "type": "SHORT_ANSWER",
      "question": "Open-ended question testing understanding of notes?",
      "options": [],
      "correctOptionIndex": 0,
      "explanation": "Model answer and criteria for a correct answer."
    }
  ]
}`;

            const resultText = await callAiWithFallback(prompt, 'You are an educational AI. Ground all output strictly in the provided text. Return valid JSON only.', true);
            const packageJson = JSON.parse(resultText);
            sendJson(200, packageJson);
            return;
        }

        // Explain Concept
        if (pathname === '/api/explain-concept' && req.method === 'POST') {
            const body = await readJsonBody();
            const { concept, context, style = 'ANALOGY' } = body;

            if (!concept) {
                sendJson(400, { error: 'concept is required' });
                return;
            }

            const prompt = `Explain the concept "${concept}" in the context of:
"""
${context || 'General educational study'}
"""

Pedagogical Style requested: ${style} (SIMPLE, ANALOGY, TECHNICAL, or APPLICATION).
Provide a clear, engaging, multi-paragraph explanation tailored to this style.`;

            const result = await callAiWithFallback(prompt, 'You are a master educator. Provide insightful, grounded explanations.', false);
            sendJson(200, { concept, style, explanation: result.trim() });
            return;
        }

        // Evaluate Short Answer
        if (pathname === '/api/evaluate-short-answer' && req.method === 'POST') {
            const body = await readJsonBody();
            const { question, modelAnswer, studentAnswer } = body;

            if (!modelAnswer || !studentAnswer) {
                sendJson(400, { error: 'modelAnswer and studentAnswer are required' });
                return;
            }

            const prompt = `Question: "${question || 'Study Question'}"
Reference Solution: "${modelAnswer}"
Student Answer: "${studentAnswer}"

Evaluate the student answer compared to the reference solution.
Return JSON:
{
  "status": "CORRECT" | "PARTIALLY_CORRECT" | "NEEDS_IMPROVEMENT",
  "feedback": "constructive 1-2 sentence feedback",
  "missingElements": ["term or concept missed 1"],
  "suggestedAnswer": "${modelAnswer}"
}`;

            const result = await callAiWithFallback(prompt, 'You are an accurate educational grading AI. Return strictly valid JSON.', true);
            sendJson(200, JSON.parse(result));
            return;
        }

        sendJson(404, { error: 'Route not found', path: pathname });
    } catch (err) {
        console.error(`[Error] ${pathname}:`, err);
        sendJson(500, {
            error: err.message || 'Internal Server Error',
            path: pathname
        });
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`=========================================`);
    console.log(`AI Study Notes Backend running on port ${PORT}`);
    console.log(`Ready to receive requests`);
    console.log(`=========================================`);
});
