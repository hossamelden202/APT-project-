# Patronus Charm — Search Engine

A full-stack search engine built from scratch. Crawls the web, indexes documents using an inverted index with TF-IDF and PageRank ranking, and serves results through a REST API to a React frontend.

## Demo

https://apt-project-kappa.vercel.app/

> See [video.mkv](video.mkv) for a full walkthrough of the system.

---

## Architecture

```
Browser (React/Vite)
       │
       ▼
Node.js Express API  ──► Java QueryProcessor ──► MongoDB Atlas
       │                        │
       │                  Inverted Index
       │                  TF-IDF + PageRank
       │
       └──► MongoDB Atlas (snippets)
```

### Components

| Component | Stack | Role |
|-----------|-------|------|
| Frontend | React 18, Vite, Tailwind CSS | Search UI, results, pagination, dark mode |
| Backend | Node.js, Express | REST API, caches results, fetches snippets |
| Search Engine | Java, Jsoup | Query processing, ranking, MongoDB queries |
| Indexer | Java, MongoDB | Parses HTML, builds inverted index |
| Crawler | Java | Multi-threaded web crawler |
| Database | MongoDB Atlas | Stores index, document metadata, PageRank |

---

## Frontend

Built with React + Vite + Tailwind CSS. Deployed on Vercel.

**Features:**
- Real-time search with loading states
- Autocomplete suggestions from the actual index
- Search timer showing query duration
- Dark mode with localStorage persistence
- Pagination (10 results per page)
- Result cards with title, URL, snippet, and score

**Structure:**
```
frontend/
├── src/
│   ├── components/
│   │   ├── SearchBox.jsx       input + suggestions dropdown
│   │   ├── ResultsList.jsx     result cards
│   │   ├── Pagination.jsx      prev/next controls
│   │   └── DarkToggle.jsx      theme toggle
│   ├── pages/
│   │   └── Home.jsx            main page, holds all state
│   ├── App.jsx                 React Router setup
│   └── main.jsx                entry point
├── vite.config.js              dev proxy to backend
└── tailwind.config.js
```

**Running locally:**
```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
```

---

## Backend

Node.js Express server. Deployed on Railway.

**Endpoints:**

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/search` | Run a search query |
| GET | `/api/suggest?q=term` | Autocomplete suggestions |
| GET | `/api/health` | Health check |

**Search request:**
```json
POST /api/search
{ "query": "car racing" }
```

**Search response:**
```json
{
  "query": "car racing",
  "results": [
    {
      "docId": "www_topendsports_com_sport_list_car_racing_sports_htm",
      "title": "www.topendsports.com/sport/list/car-racing-sports.htm",
      "url": "https://www.topendsports.com/sport/list/car-racing-sports.htm",
      "snippet": "...preview text from document...",
      "score": 1.5
    }
  ],
  "cached": false
}
```

The backend spawns a Java `QueryProcessor` process per query, parses its stdout, then enriches results with snippets fetched from MongoDB.

**Running locally:**
```bash
cd backend
npm install
npm start          # http://localhost:8080
```

---

## Search Engine (Java)

The core search engine is written in Java and lives in `indexer/` and `Rankers/`.

**Indexing pipeline:**
1. Crawler fetches HTML pages into `data/crawled_pages/`
2. Indexer parses HTML with Jsoup, tokenizes, removes stop words, applies Porter stemming
3. Builds an inverted index: `word → [(docId, freq_title, freq_body, paragraph)]`
4. Saves index to MongoDB `documents2` collection
5. Saves document metadata (URL, length, PageRank) to `documents3`

**Query processing:**
1. `QueryProcessor` receives query string as CLI argument
2. Preprocesses: lowercase → stop word removal → stemming
3. Fetches matching postings from MongoDB
4. Passes to `Ranker` which scores using TF-IDF + PageRank
5. Prints results to stdout in `Doc ID / URL / Score` format

**Ranking factors:**
- TF-IDF (term frequency in title weighted higher than body)
- PageRank score from pre-computed CSV
- Phrase matching bonus for exact phrase queries

**Recompiling (only if source changes):**
```bash
cd ~/APT-project-
javac -cp ".:lib/*" indexer/*.java Rankers/*.java
```

---

## Database (MongoDB Atlas)

Two collections in `indexerdb`:

**`documents2`** — inverted index entries:
```
{ w: "stemmed_word", dId: "doc_id", fh: 1, fb: 3, p: "snippet text..." }
```

**`documents3`** — document metadata:
```
{ did: "doc_id", url: "https://...", length: 341, pR: "0.00123" }
```

---

## Deployment

### Frontend → Vercel

1. Push repo to GitHub
2. Import project on [vercel.com](https://vercel.com)
3. Root directory: `frontend`
4. Framework preset: Vite
5. Add environment variable: `VITE_API_URL=https://your-railway-url.up.railway.app`
6. Deploy

### Backend → Railway

1. Import repo on [railway.app](https://railway.app)
2. Root directory: `backend`
3. Railway auto-detects `railway.toml` and uses the Dockerfile
4. Add environment variable: `PROJECT_ROOT=/app`
5. Deploy

The `backend/Dockerfile` installs Node 20 + OpenJDK 17. The compiled `.class` files and `lib/` jars are included in `backend/` for Railway to find them.

---

## Local Development

Prerequisites: Node.js 20+, Java 17+, MongoDB (or Atlas connection)

```bash
# Terminal 1 — backend
cd backend && npm install && npm start

# Terminal 2 — frontend
cd frontend && npm install && npm run dev
```

Open `http://localhost:3000`. The Vite dev server proxies `/api/*` to `http://localhost:8080`.

---

## Tech Stack

- **Frontend**: React 18, Vite, Tailwind CSS, React Router
- **Backend**: Node.js, Express, MongoDB Node driver
- **Search**: Java 17, Jsoup, Porter Stemmer
- **Database**: MongoDB Atlas
- **Deployment**: Vercel (frontend), Railway (backend)

---

Built by **HossamElden Mohamed** — [GitHub](https://github.com/hossamelden202) · [LinkedIn](https://www.linkedin.com/in/hossam-elden-mohamed/)
