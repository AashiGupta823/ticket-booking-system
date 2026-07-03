import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './auth.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import Events from './pages/Events.jsx'
import ShowDetail from './pages/ShowDetail.jsx'
import MyBookings from './pages/MyBookings.jsx'
import WaitlistOfferPage from './pages/WaitlistOffer.jsx'
import CreateVenue from './pages/CreateVenue.jsx'
import CreateShow from './pages/CreateShow.jsx'

function Nav() {
  const { user, logout } = useAuth()
  const nav = useNavigate()
  return (
    <div style={{ display: 'flex', gap: 16, padding: '12px 20px', borderBottom: '1px solid #2a2d34', alignItems: 'center' }}>
      <Link to="/" style={{ color: '#eee', fontWeight: 700, textDecoration: 'none' }}>🎟 TicketBooking</Link>
      <Link to="/" style={{ color: '#aaa' }}>Events</Link>
      {user && <Link to="/my-bookings" style={{ color: '#aaa' }}>My Bookings</Link>}
      {user && (user.role === 'ADMIN') && <Link to="/admin/venues/new" style={{ color: '#aaa' }}>New Venue</Link>}
      {user && (user.role === 'ORGANISER' || user.role === 'ADMIN') && <Link to="/organiser/shows/new" style={{ color: '#aaa' }}>New Show</Link>}
      <div style={{ marginLeft: 'auto' }}>
        {user ? (
          <>
            <span style={{ marginRight: 12, color: '#888' }}>{user.name} ({user.role})</span>
            <button onClick={() => { logout(); nav('/login') }}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/login" style={{ color: '#4ea1ff', marginRight: 12 }}>Login</Link>
            <Link to="/register" style={{ color: '#4ea1ff' }}>Register</Link>
          </>
        )}
      </div>
    </div>
  )
}

function App() {
  return (
    <BrowserRouter>
      <Nav />
      <div style={{ padding: 20, maxWidth: 960, margin: '0 auto' }}>
        <Routes>
          <Route path="/" element={<Events />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/shows/:id" element={<ShowDetail />} />
          <Route path="/my-bookings" element={<MyBookings />} />
          <Route path="/waitlist-offer/:token" element={<WaitlistOfferPage />} />
          <Route path="/admin/venues/new" element={<CreateVenue />} />
          <Route path="/organiser/shows/new" element={<CreateShow />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <AuthProvider>
    <App />
  </AuthProvider>
)
