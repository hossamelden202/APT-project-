export default function ResultsList({ results, searchMeta }) {
  if (!results.length) return null

  return (
    <div>
      {searchMeta && (
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">{searchMeta}</p>
      )}
      <div className="flex flex-col divide-y divide-gray-200 dark:divide-gray-700">
        {results.map((r, i) => (
          <ResultCard key={r.docId || i} result={r} />
        ))}
      </div>
    </div>
  )
}

function ResultCard({ result }) {
  return (
    <div className="py-5 transition-transform hover:translate-x-1">
      <a
        href={result.url || '#'}
        target="_blank"
        rel="noopener noreferrer"
        className="block text-lg font-medium mb-1 transition-colors
          text-accent hover:text-blue-800 hover:underline
          dark:text-accent-dark dark:hover:text-blue-300"
      >
        {result.title || result.docId || 'Untitled'}
      </a>
      <p className="text-sm text-green-700 dark:text-green-400 mb-1.5 break-all">
        {result.url}
      </p>
      {result.snippet && (
        <p className="text-sm text-gray-600 dark:text-gray-400 leading-relaxed mb-2">
          {result.snippet}
        </p>
      )}
      <span className="inline-block text-xs text-gray-500 dark:text-gray-500 bg-gray-100 dark:bg-gray-800 px-2 py-1 rounded">
        Score: {(result.score || 0).toFixed(4)}
      </span>
    </div>
  )
}
