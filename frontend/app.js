const state = {
    query: '',
    results: [],
    currentPage: 1,
    resultsPerPage: 10,
    totalResults: 0,
    isLoading: false,
    searchTime: 0,
};

const API_URL = "http://localhost:8080/api/search";

const searchInput = document.getElementById('searchInput');
const searchBtn = document.getElementById('searchBtn');
const resultsContainer = document.getElementById('resultsContainer');
const emptyState = document.getElementById('emptyState');
const loadingState = document.getElementById('loadingState');
const errorState = document.getElementById('errorState');
const errorMessage = document.getElementById('errorMessage');
const resultsList = document.getElementById('resultsList');
const paginationContainer = document.getElementById('paginationContainer');
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const pageInfo = document.getElementById('pageInfo');
const searchMeta = document.getElementById('searchMeta');
const suggestions = document.getElementById('suggestions');
const darkToggle = document.getElementById('darkToggle');

let suggestTimer = null;

// dark mode
const savedTheme = localStorage.getItem('theme');
if (savedTheme === 'dark') document.body.classList.add('dark');

darkToggle.addEventListener('click', () => {
    document.body.classList.toggle('dark');
    localStorage.setItem('theme', document.body.classList.contains('dark') ? 'dark' : 'light');
});

searchBtn.addEventListener('click', performSearch);
searchInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') performSearch();
});

searchInput.addEventListener('input', () => {
    const val = searchInput.value.trim();
    if (!val || val.length < 2) { suggestions.style.display = 'none'; return; }

    clearTimeout(suggestTimer);
    suggestTimer = setTimeout(async () => {
        try {
            const res = await fetch(`http://localhost:8080/api/suggest?q=${encodeURIComponent(val)}`);
            const data = await res.json();
            const words = data.suggestions || [];

            if (words.length === 0) { suggestions.style.display = 'none'; return; }

            suggestions.innerHTML = '';
            words.forEach(w => {
                const item = document.createElement('div');
                item.className = 'suggestion-item';
                item.textContent = w;
                item.addEventListener('click', () => {
                    searchInput.value = w;
                    suggestions.style.display = 'none';
                    performSearch();
                });
                suggestions.appendChild(item);
            });
            suggestions.style.display = 'block';
        } catch { suggestions.style.display = 'none'; }
    }, 250); // debounce 250ms
});

document.addEventListener('click', (e) => {
    if (!suggestions.contains(e.target) && e.target !== searchInput) {
        suggestions.style.display = 'none';
    }
});

async function performSearch() {
    const query = searchInput.value.trim();
    suggestions.style.display = 'none';
    if (!query) { showEmptyState(); return; }
    state.query = query;
    state.currentPage = 1;
    await executeSearch(query);
}

async function executeSearch(query) {
    state.isLoading = true;
    showLoadingState();

    const startTime = performance.now();

    try {
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ query }),
        });

        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

        const data = await response.json();
        state.searchTime = ((performance.now() - startTime) / 1000).toFixed(2);

        if (!data.results || data.results.length === 0) { showEmptyResults(); return; }

        state.results = data.results;
        state.totalResults = data.results.length;
        renderResults();
        updatePagination();

    } catch (error) {
        console.error('Search error:', error);
        showError(`Unable to reach the search backend. Error: ${error.message}`);
    } finally {
        state.isLoading = false;
    }
}

function renderResults() {
    resultsList.innerHTML = '';
    paginationContainer.style.display = state.totalResults > state.resultsPerPage ? 'flex' : 'none';

    const startIndex = (state.currentPage - 1) * state.resultsPerPage;
    const pageResults = state.results.slice(startIndex, startIndex + state.resultsPerPage);

    if (pageResults.length === 0) { showEmptyResults(); return; }

    emptyState.style.display = 'none';
    loadingState.style.display = 'none';
    errorState.style.display = 'none';
    searchMeta.style.display = 'block';
    searchMeta.textContent = `About ${state.totalResults} results (${state.searchTime} seconds)`;

    pageResults.forEach(result => resultsList.appendChild(createResultElement(result)));
}

function createResultElement(result) {
    const div = document.createElement('div');
    div.className = 'result-item';

    const title = document.createElement('a');
    title.className = 'result-title';
    title.href = result.url || '#';
    title.target = '_blank';
    title.textContent = result.title || result.docId || 'Untitled';

    const url = document.createElement('div');
    url.className = 'result-url';
    url.textContent = result.url || '';

    const snippet = document.createElement('div');
    snippet.className = 'result-snippet';
    snippet.textContent = result.snippet || 'No preview available';

    const meta = document.createElement('div');
    meta.className = 'result-meta';
    const score = document.createElement('span');
    score.className = 'result-score';
    score.textContent = `Score: ${(result.score || 0).toFixed(4)}`;
    meta.appendChild(score);

    div.appendChild(title);
    div.appendChild(url);
    div.appendChild(snippet);
    div.appendChild(meta);
    return div;
}

function updatePagination() {
    const totalPages = Math.ceil(state.totalResults / state.resultsPerPage);
    prevBtn.disabled = state.currentPage <= 1;
    nextBtn.disabled = state.currentPage >= totalPages;
    pageInfo.textContent = `Page ${state.currentPage} of ${totalPages} (${state.totalResults} results)`;
}

function goToPreviousPage() {
    if (state.currentPage > 1) { state.currentPage--; renderResults(); updatePagination(); window.scrollTo(0, 0); }
}

function goToNextPage() {
    const totalPages = Math.ceil(state.totalResults / state.resultsPerPage);
    if (state.currentPage < totalPages) { state.currentPage++; renderResults(); updatePagination(); window.scrollTo(0, 0); }
}

prevBtn.addEventListener('click', goToPreviousPage);
nextBtn.addEventListener('click', goToNextPage);

function showEmptyState() {
    resultsList.innerHTML = '';
    searchMeta.style.display = 'none';
    emptyState.innerHTML = '<p>Enter a search query to get started</p>';
    emptyState.style.display = 'block';
    loadingState.style.display = 'none';
    errorState.style.display = 'none';
    paginationContainer.style.display = 'none';
}

function showEmptyResults() {
    resultsList.innerHTML = '';
    searchMeta.style.display = 'none';
    emptyState.innerHTML = '<p>No results found. Try a different query.</p>';
    emptyState.style.display = 'block';
    loadingState.style.display = 'none';
    errorState.style.display = 'none';
    paginationContainer.style.display = 'none';
}

function showLoadingState() {
    resultsList.innerHTML = '';
    searchMeta.style.display = 'none';
    emptyState.style.display = 'none';
    loadingState.style.display = 'flex';
    errorState.style.display = 'none';
    paginationContainer.style.display = 'none';
}

function showError(message) {
    resultsList.innerHTML = '';
    searchMeta.style.display = 'none';
    emptyState.style.display = 'none';
    loadingState.style.display = 'none';
    errorState.style.display = 'block';
    errorMessage.textContent = message;
    paginationContainer.style.display = 'none';
}

window.addEventListener('DOMContentLoaded', () => showEmptyState());
