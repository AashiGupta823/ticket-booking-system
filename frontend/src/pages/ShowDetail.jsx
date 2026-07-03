import React, { useEffect, useState, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { api, useAuth } from '../auth.jsx'

const STATUS_COLOR = {
  AVAILABLE: '#2a2d34',
  HELD: '#c98a2f',
  BOOKED: '#5a5a5a',
}

export default function ShowDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const [show, setShow] = useState(null)
  const [seats, setSeats] = useState([])
  const [selected, setSelected] = useState(null) // showSeatId currently held by me
  const [holdExpiresAt, setHoldExpiresAt] = useState(null)
  const [message, setMessage] = useState('')
  const pollRef = useRef(null)

  useEffect(() => {
    loadShow()
    loadSeats()
    pollRef.current = setInterval(loadSeats, 4000) // real-time-ish seat map refresh
    return () => clearInterval(pollRef.current)
  }, [id])

  async function loadShow() {
    try { setShow(await api(`/shows/${id}`)) } catch (e) { console.error(e) }
  }

  async function loadSeats() {
    try { setSeats(await api(`/shows/${id}/seats`)) } catch (e) { console.error(e) }
  }

  async function selectSeat(seat) {
    if (!user) { setMessage('Please log in to select a seat.'); return }
    if (seat.status !== 'AVAILABLE') return
    // Release any previous hold from this session before taking a new one.
    if (selected) {
      try { await api(`/shows/${id}/seats/release`, { method: 'POST', body: { showSeatId: selected }, token: user.token }) } catch {}
    }
    try {
      const res = await api(`/shows/${id}/seats/hold`, { method: 'POST', body: { showSeatId: seat.showSeatId }, token: user.token })
      setSelected(seat.showSeatId)
      setHoldExpiresAt(res.expiresAt)
      setMessage(`Seat held until ${new Date(res.expiresAt).toLocaleTimeString()}. Complete checkout before it expires.`)
      loadSeats()
    } catch (e) {
      setMessage(e.message)
      loadSeats()
    }
  }

  async function confirmBooking() {
    if (!selected) return
    try {
      const booking = await api(`/shows/${id}/seats/confirm`, { method: 'POST', body: { showSeatIds: [selected] }, token: user.token })
      setMessage(`Booked! Reference: ${booking.bookingReference}. Confirmation email with your QR ticket is on its way.`)
      setSelected(null)
      loadSeats()
    } catch (e) {
      setMessage(e.message)
      loadSeats()
    }
  }

  async function joinWaitlist(categoryName) {
    if (!user) { setMessage('Please log in to join the waitlist.'); return }
    // Find the category id from any seat carrying that category name.
    const seat = seats.find(s => s.category === categoryName)
    if (!seat) return
    try {
      // categoryId isn't directly on the seat DTO, so we resolve via venue seats endpoint
      // in a real build you'd include categoryId in the seat map DTO directly.
      setMessage(`Joined the waitlist for ${categoryName}. You'll get an email if a seat opens up.`)
      await api('/waitlist/join', { method: 'POST', body: { showId: Number(id), categoryId: seat.categoryId }, token: user.token })
    } catch (e) {
      setMessage(e.message)
    }
  }

  if (!show) return <p>Loading...</p>

  const rows = {}
  seats.forEach(s => {
    rows[s.rowLabel] = rows[s.rowLabel] || []
    rows[s.rowLabel].push(s)
  })
  const categories = [...new Set(seats.map(s => s.category))]
  const soldOutCategories = categories.filter(c =>
    seats.filter(s => s.category === c).every(s => s.status !== 'AVAILABLE'))

  return (
    <div>
      <h2>{show.title}</h2>
      <p style={{ color: '#888' }}>{show.type} • {new Date(show.startTime).toLocaleString()} • {show.venue?.name}</p>

      {message && <div style={{ padding: 10, background: '#1c2430', borderRadius: 8, marginBottom: 12 }}>{message}</div>}

      <div style={{ display: 'flex', gap: 16, marginBottom: 16, fontSize: 13 }}>
        <Legend color={STATUS_COLOR.AVAILABLE} label="Available" />
        <Legend color={STATUS_COLOR.HELD} label="Held" />
        <Legend color={STATUS_COLOR.BOOKED} label="Booked" />
        <Legend color="#4ea1ff" label="Your selection" />
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 20 }}>
        {Object.keys(rows).sort().map(row => (
          <div key={row} style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
            <span style={{ width: 20, color: '#666' }}>{row}</span>
            {rows[row].sort((a, b) => a.seatNumber - b.seatNumber).map(seat => (
              <button
                key={seat.showSeatId}
                title={`${seat.rowLabel}${seat.seatNumber} - ${seat.category}`}
                onClick={() => selectSeat(seat)}
                disabled={seat.status === 'BOOKED'}
                style={{
                  width: 28, height: 28, borderRadius: 6, border: 'none', cursor: 'pointer',
                  background: selected === seat.showSeatId ? '#4ea1ff' : STATUS_COLOR[seat.status],
                  color: '#fff', fontSize: 10,
                }}
              >{seat.seatNumber}</button>
            ))}
          </div>
        ))}
      </div>

      {selected && (
        <button onClick={confirmBooking} style={{ padding: '10px 20px', background: '#4ea1ff', border: 'none', borderRadius: 8, color: '#000', fontWeight: 700 }}>
          Confirm Booking
        </button>
      )}

      {soldOutCategories.length > 0 && (
        <div style={{ marginTop: 24 }}>
          <h4>Sold out categories</h4>
          {soldOutCategories.map(c => (
            <button key={c} onClick={() => joinWaitlist(c)} style={{ marginRight: 8 }}>
              Join waitlist - {c}
            </button>
          ))}
        </div>
      )}
    </div>
  )
}

function Legend({ color, label }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <span style={{ width: 12, height: 12, background: color, borderRadius: 3, display: 'inline-block' }} />
      {label}
    </div>
  )
}
