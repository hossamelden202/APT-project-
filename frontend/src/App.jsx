import { Routes, Route } from 'react-router-dom'
import Home from './pages/Home'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Home />} />
      {/* add future pages here e.g. <Route path="/about" element={<About />} /> */}
    </Routes>
  )
}
