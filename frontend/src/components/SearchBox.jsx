import { useState, useEffect, useRef } from 'react'

export default function SearchBox({ onSearch }) {
  const [query, setQuery] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const timerRef = useRef(null)
  const wrapperRef = useRef(null)

  useEffect(() => {
    function handleClickOutside(e) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  function handleInput(e) {
    const val = e.target.value
    setQuery(val)

    clearTimeout(timerRef.current)
    if (val.trim().length < 2) { setSuggestions([]); setShowSuggestions(false); return }

    timerRef.current = setTimeout(async () => {
      try {
        const res = await fetch(`${import.meta.env.VITE_API_URL || ''}/api/suggest?q=${encodeURIComponent(val.trim())}`)
        const data = await res.json()
        setSuggestions(data.suggestions || [])
        setShowSuggestions((data.suggestions || []).length > 0)
      } catch { setSuggestions([]); setShowSuggestions(false) }
    }, 250)
  }

  function handleSearch() {
    const q = query.trim()
    if (!q) return
    setShowSuggestions(false)
    onSearch(q)
  }

  function handleSuggestionClick(word) {
    setQuery(word)
    setShowSuggestions(false)
    onSearch(word)
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter') handleSearch()
  }

  return (
    <div ref={wrapperRef} className="relative w-full max-w-2xl mx-auto">
      <div className="flex gap-2">
        <input
          type="text"
          value={query}
          onChange={handleInput}
          onKeyDown={handleKeyDown}
          placeholder="Enter your search query..."
          autoComplete="off"
          className="flex-1 px-5 py-3 text-base rounded-full border outline-none transition-colors
            bg-gray-50 border-gray-300 text-gray-900 placeholder-gray-400
            focus:border-gray-900 focus:bg-white
            dark:bg-gray-800 dark:border-gray-600 dark:text-gray-100 dark:placeholder-gray-500
            dark:focus:border-gray-300 dark:focus:bg-gray-700"
        />
        <button
          onClick={handleSearch}
          className="px-6 py-3 text-base font-medium rounded-full transition-colors
            bg-gray-900 text-white hover:bg-gray-700
            dark:bg-gray-100 dark:text-gray-900 dark:hover:bg-gray-300"
        >
          Search
        </button>
      </div>

      {showSuggestions && (
        <ul className="absolute z-10 top-full mt-1 left-0 right-20 rounded-lg border shadow-md overflow-hidden
          bg-white border-gray-200
          dark:bg-gray-800 dark:border-gray-700">
          {suggestions.map((w, i) => (
            <li
              key={i}
              onClick={() => handleSuggestionClick(w)}
              className="px-4 py-2.5 text-sm cursor-pointer transition-colors
                text-gray-800 hover:bg-gray-100
                dark:text-gray-200 dark:hover:bg-gray-700"
            >
              {w}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
