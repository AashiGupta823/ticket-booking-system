import React, { useEffect, useState } from 'react'
import { api, useAuth } from '../auth.jsx'

export default function MyBookings() {
  const { user } = useAuth()
  const [bookings, setBookings] = useState([])
  const [message, setMessage] = useState('')

  useEffect(() => { if (user) load() }, [user])

  async function load() {
    try { setBookings(await api('/bookings/me', { token: user.token })) } catch (e) { console.error(e) }
  }

  async function cancel(id) {
    try {
      await api(`/bookings/${id}/cancel`, { method: 'POST', token: user.token })
      setMessage('Booking cancelled. The seat has been offered to the next person on the waitlist, if any.')
      load()
    } catch (e) { setMessage(e.message) }
  }

  if (!user) return <p>Please log in to view your bookings.</p>

  return (
    <div>
      <h2>My Bookings</h2>
      {message && <div style={{ padding: 10, background: '#1c2430', borderRadius: 8, marginBottom: 12 }}>{message}</div>}
      {bookings.map(b => (
        <div key={b.id} style={{ padding: 14, border: '1px solid #2a2d34', borderRadius: 10, marginBottom: 10 }}>
          <div style={{ fontWeight: 600 }}>{b.show?.title} — {b.bookingReference}</div>
          <div style={{ color: '#888', fontSize: 13 }}>Status: {b.status} • Total: ₹{b.totalAmount}</div>
          <div style={{ color: '#888', fontSize: 13 }}>Seats: {b.seats?.map(s => `${s.showSeat.seat.rowLabel}${s.showSeat.seat.seatNumber}`).join(', ')}</div>
          {b.status === 'CONFIRMED' && <button onClick={() => cancel(b.id)} style={{ marginTop: 8 }}>Cancel Booking</button>}
        </div>
      ))}
      {bookings.length === 0 && <p style={{ color: '#888' }}>No bookings yet.</p>}
    </div>
  )
}
