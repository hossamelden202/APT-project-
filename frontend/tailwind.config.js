/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        accent: '#1976d2',
        'accent-dark': '#90caf9',
      }
    }
  },
  plugins: []
}
