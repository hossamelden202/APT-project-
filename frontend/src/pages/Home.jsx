import { useState, useEffect } from 'react'
import SearchBox from '../components/SearchBox'
import ResultsList from '../components/ResultsList'
import Pagination from '../components/Pagination'
import DarkToggle from '../components/DarkToggle'

const RESULTS_PER_PAGE = 10

export default function Home() {
  const [dark, setDark] = useState(() => localStorage.getItem('theme') === 'dark')
  const [results, setResults] = useState([])
  const [status, setStatus] = useState('idle') // idle | loading | error | done
  const [errorMsg, setErrorMsg] = useState('')
  const [searchMeta, setSearchMeta] = useState('')
  const [currentPage, setCurrentPage] = useState(1)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
    localStorage.setItem('theme', dark ? 'dark' : 'light')
  }, [dark])

  async function handleSearch(query) {
    setStatus('loading')
    setResults([])
    setCurrentPage(1)
    setSearchMeta('')

    const start = performance.now()

    try {
      const res = await fetch('/api/search', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ query }),
      })

      if (!res.ok) throw new Error(`HTTP ${res.status}`)

      const data = await res.json()
      const elapsed = ((performance.now() - start) / 1000).toFixed(2)

      if (!data.results || data.results.length === 0) {
        setStatus('done')
        setResults([])
        setSearchMeta('No results found.')
        return
      }

      setResults(data.results)
      setSearchMeta(`About ${data.results.length} results (${elapsed} seconds)`)
      setStatus('done')
    } catch (err) {
      setErrorMsg(`Unable to reach the search backend. ${err.message}`)
      setStatus('error')
    }
  }

  const totalPages = Math.ceil(results.length / RESULTS_PER_PAGE)
  const pageResults = results.slice(
    (currentPage - 1) * RESULTS_PER_PAGE,
    currentPage * RESULTS_PER_PAGE
  )

  function handlePrev() { setCurrentPage(p => p - 1); window.scrollTo(0, 0) }
  function handleNext() { setCurrentPage(p => p + 1); window.scrollTo(0, 0) }

  return (
    <div className="min-h-screen bg-white text-gray-900 dark:bg-gray-950 dark:text-gray-100 transition-colors">
      <div className="max-w-3xl mx-auto px-5 py-6 flex flex-col min-h-screen">

        {/* top bar */}
        <div className="flex justify-end mb-3">
          <DarkToggle dark={dark} onToggle={() => setDark(d => !d)} />
        </div>

        {/* header */}
        <div className="text-center pb-8 border-b border-gray-200 dark:border-gray-700 mb-6">
          <h1 className="text-5xl font-bold tracking-tight mb-2">
            Patronus <span className="text-accent dark:text-accent-dark">Charm</span>
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400 mb-6">Fast. Smart. Yours.</p>
          <SearchBox onSearch={handleSearch} />
        </div>

        {/* content */}
        <div className="flex-1">
          {status === 'idle' && (
            <p className="text-center text-gray-400 dark:text-gray-500 mt-16 text-lg">
              Enter a search query to get started
            </p>
          )}

          {status === 'loading' && (
            <div className="flex flex-col items-center gap-4 mt-16">
              <div className="w-10 h-10 border-3 border-gray-200 border-t-gray-900 dark:border-gray-700 dark:border-t-gray-200 rounded-full animate-spin" />
              <p className="text-gray-500 dark:text-gray-400">Searching...</p>
            </div>
          )}

          {status === 'error' && (
            <div className="mt-6 p-4 bg-red-50 dark:bg-red-950 border-l-4 border-red-600 rounded">
              <p className="text-red-700 dark:text-red-400 font-medium">{errorMsg}</p>
            </div>
          )}

          {status === 'done' && (
            <>
              <ResultsList results={pageResults} searchMeta={searchMeta} />
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPrev={handlePrev}
                onNext={handleNext}
              />
            </>
          )}
        </div>

        {/* footer */}
        <footer className="flex flex-col items-center gap-3 text-center pt-7 mt-10 border-t border-gray-200 dark:border-gray-700">
          <p className="text-xs text-gray-400 dark:text-gray-600">Powered by APT Search Engine</p>
          <div className="flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400 flex-wrap justify-center">
            <span>Built by <strong className="text-gray-700 dark:text-gray-200">HossamElden Mohamed</strong></span>
            <span className="w-px h-3.5 bg-gray-300 dark:bg-gray-600 inline-block" />
            <div className="flex items-center gap-2">
              <a
                href="https://github.com/hossamelden202"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400 border border-gray-300 dark:border-gray-600 rounded-full px-3 py-1 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577v-2.165c-3.338.726-4.042-1.61-4.042-1.61-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.085 1.84 1.237 1.84 1.237 1.07 1.834 2.807 1.304 3.492.997.108-.775.418-1.305.762-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.468-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.3 1.23a11.5 11.5 0 0 1 3.003-.404c1.02.005 2.047.138 3.006.404 2.29-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.61-2.807 5.625-5.479 5.92.43.372.823 1.102.823 2.222v3.293c0 .322.218.694.825.576C20.565 21.796 24 17.298 24 12c0-6.63-5.37-12-12-12z"/></svg>
                GitHub
              </a>
              <span className="text-gray-300 dark:text-gray-600">·</span>
              <a
                href="https://www.linkedin.com/in/hossam-elden-mohamed/"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-xs text-gray-500 dark:text-gray-400 border border-gray-300 dark:border-gray-600 rounded-full px-3 py-1 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="13" height="13" viewBox="0 0 24 24" fill="currentColor"><path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 0 1-2.063-2.065 2.064 2.064 0 1 1 2.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/></svg>
                LinkedIn
              </a>
            </div>
          </div>
        </footer>

      </div>
    </div>
  )
}
