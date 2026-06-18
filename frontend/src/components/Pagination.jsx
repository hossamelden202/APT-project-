export default function Pagination({ currentPage, totalPages, onPrev, onNext }) {
  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-center gap-5 mt-10 pt-5 border-t border-gray-200 dark:border-gray-700">
      <button
        onClick={onPrev}
        disabled={currentPage <= 1}
        className="px-4 py-2 text-sm font-medium rounded border transition-colors
          border-gray-300 text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed
          dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
      >
        Previous
      </button>
      <span className="text-sm text-gray-500 dark:text-gray-400 min-w-[120px] text-center">
        Page {currentPage} of {totalPages}
      </span>
      <button
        onClick={onNext}
        disabled={currentPage >= totalPages}
        className="px-4 py-2 text-sm font-medium rounded border transition-colors
          border-gray-300 text-gray-700 hover:bg-gray-100 disabled:opacity-40 disabled:cursor-not-allowed
          dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
      >
        Next
      </button>
    </div>
  )
}
