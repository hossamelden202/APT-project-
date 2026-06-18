export default function DarkToggle({ dark, onToggle }) {
  return (
    <button
      onClick={onToggle}
      className="px-4 py-1.5 text-sm border rounded-full transition-colors
        border-gray-300 text-gray-600 hover:bg-gray-100
        dark:border-gray-600 dark:text-gray-300 dark:hover:bg-gray-800"
    >
      {dark ? 'Light Mode' : 'Dark Mode'}
    </button>
  )
}
