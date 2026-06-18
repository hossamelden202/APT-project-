const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const { spawn } = require('child_process');
const path = require('path');
const { MongoClient } = require('mongodb');
require('dotenv').config();
const MONGO_URI = process.env.MONGO_URI;
const mongoClient = new MongoClient(MONGO_URI);
let snippetCollection = null;
let wordCollection = null;

mongoClient.connect().then(() => {
    const db = mongoClient.db('indexerdb');
    snippetCollection = db.collection('documents2');
    wordCollection = db.collection('documents2'); // words are also in documents2 under field "w"
    console.log('Node connected to MongoDB for snippets');
}).catch(err => {
    console.error('Node MongoDB connection failed:', err.message);
});

const app = express();
const PORT = process.env.PORT || 8080;

app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ limit: '50mb', extended: true }));

const resultCache = new Map();
const suggestCache = new Map();

const PROJECT_ROOT = process.env.PROJECT_ROOT || path.join(__dirname, '..');
const LIB_DIR = path.join(PROJECT_ROOT, 'lib');
const CLASSPATH = [
    PROJECT_ROOT,
    `${LIB_DIR}/mongodb-driver-sync-3.12.14.jar`,
    `${LIB_DIR}/mongodb-driver-core-3.12.14.jar`,
    `${LIB_DIR}/bson-3.12.14.jar`,
    `${LIB_DIR}/jackson-core-2.12.4.jar`,
    `${LIB_DIR}/jackson-databind-2.12.4.jar`,
    `${LIB_DIR}/jackson-annotations-2.12.4.jar`,
    `${LIB_DIR}/jsoup-1.19.1.jar`,
].join(':');

function executeJavaSearch(query) {
    return new Promise((resolve, reject) => {
        console.log('Spawning Java QueryProcessor for query:', query);

        const javaProcess = spawn('java', [
            '-cp', CLASSPATH,
            'indexer.QueryProcessor',
            query
        ], { cwd: PROJECT_ROOT });

        let output = '';
        let errorOutput = '';

        javaProcess.stdout.on('data', (data) => { output += data.toString(); });
        javaProcess.stderr.on('data', (data) => {
            errorOutput += data.toString();
            console.log('[Java stderr]', data.toString().trim());
        });

        javaProcess.on('close', (code) => {
            console.log('Java process exited with code:', code);
            console.log('Java stdout:', output);

            if (code !== 0 && output.trim() === '') {
                return reject(new Error(`Java exited with code ${code}. stderr: ${errorOutput}`));
            }

            try {
                const results = parseJavaOutput(output);
                if (results.length === 0) {
                    console.warn('Java ran but returned no results. stderr was:', errorOutput);
                }
                resolve(results);
            } catch (err) {
                reject(new Error('Failed to parse Java output: ' + err.message));
            }
        });

        javaProcess.on('error', (err) => {
            reject(new Error('Failed to spawn Java: ' + err.message + '. Is java installed?'));
        });
    });
}

function parseJavaOutput(output) {
    const results = [];
    const blocks = output.split('-----------------------------');

    for (const block of blocks) {
        const lines = block.trim().split('\n');
        if (lines.length < 2) continue;

        const result = { docId: '', url: '', score: 0, title: '', snippet: '' };
        let valid = false;

        for (const line of lines) {
            const trimmed = line.trim();
            if (trimmed.startsWith('Doc ID:')) {
                result.docId = trimmed.replace('Doc ID:', '').trim();
                valid = true;
            } else if (trimmed.startsWith('URL:')) {
                result.url = trimmed.replace('URL:', '').trim();
                try {
                    const u = new URL(result.url);
                    result.title = u.hostname + u.pathname;
                } catch {
                    result.title = result.url;
                }
            } else if (trimmed.startsWith('Score:')) {
                result.score = parseFloat(trimmed.replace('Score:', '').trim()) || 0;
            }
        }

        if (valid && result.url) {
            result.snippet = '';
            results.push(result);
        }
    }

    results.sort((a, b) => b.score - a.score);
    return results.slice(0, 50);
}

// POST /api/search
app.post('/api/search', async (req, res) => {
    const { query } = req.body;

    if (!query || query.trim() === '') {
        return res.status(400).json({ error: 'Query is required', results: [] });
    }

    const trimmedQuery = query.trim();

    if (resultCache.has(trimmedQuery)) {
        console.log('Cache hit for:', trimmedQuery);
        return res.json({ query: trimmedQuery, results: resultCache.get(trimmedQuery), cached: true });
    }

    try {
        const results = await executeJavaSearch(trimmedQuery);

        if (snippetCollection) {
            const docIds = results.map(r => r.docId);
            const snippetDocs = await snippetCollection.find({ dId: { $in: docIds } }).toArray();
            const snippetMap = {};
            for (const doc of snippetDocs) {
                if (!snippetMap[doc.dId]) snippetMap[doc.dId] = doc.p || '';
            }
            for (const result of results) {
                result.snippet = snippetMap[result.docId] || '';
            }
        }

        resultCache.set(trimmedQuery, results);
        res.json({ query: trimmedQuery, results, cached: false });
    } catch (error) {
        console.error('Search error:', error.message);
        res.status(500).json({ error: 'Search failed', message: error.message, results: [] });
    }
});

// GET /api/suggest?q=car  — returns up to 8 matching words from the index
app.get('/api/suggest', async (req, res) => {
    const q = (req.query.q || '').trim().toLowerCase();

    if (!q || q.length < 2) return res.json({ suggestions: [] });
    if (suggestCache.has(q)) return res.json({ suggestions: suggestCache.get(q) });

    if (!wordCollection) return res.json({ suggestions: [] });

    try {
        const docs = await wordCollection
            .find({ w: { $regex: `^${q}`, $options: 'i' } }, { projection: { w: 1, _id: 0 } })
            .limit(50)
            .toArray();

        // deduplicate words and return top 8
        const words = [...new Set(docs.map(d => d.w).filter(Boolean))].slice(0, 8);
        suggestCache.set(q, words);
        res.json({ suggestions: words });
    } catch (err) {
        console.error('Suggest error:', err.message);
        res.json({ suggestions: [] });
    }
});

// GET /api/health
app.get('/api/health', (req, res) => {
    res.json({ status: 'ok', message: 'APT Search Backend running', timestamp: new Date().toISOString() });
});

app.get('/', (req, res) => {
    res.json({ message: 'APT Search Engine API', endpoints: { search: 'POST /api/search', suggest: 'GET /api/suggest?q=term', health: 'GET /api/health' } });
});

app.use((req, res) => { res.status(404).json({ error: 'Not Found' }); });

app.use((err, req, res, next) => {
    console.error('Unhandled error:', err);
    res.status(500).json({ error: 'Internal Server Error', message: err.message });
});

app.listen(PORT, () => {
    console.log(`APT Search Backend running on http://localhost:${PORT}`);
    console.log(`Project root: ${PROJECT_ROOT}`);
    console.log(`Classpath: ${CLASSPATH}`);
});

app.get('/api/debug', (req, res) => {
    const { execSync } = require('child_process');
    try {
        const java = execSync('which java 2>/dev/null || echo notfound').toString().trim();
        res.json({ java, PATH: process.env.PATH, cwd: process.cwd(), dirname: __dirname });
    } catch (e) {
        res.json({ error: e.message, PATH: process.env.PATH });
    }
});
