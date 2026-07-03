import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { api, useAuth } from '../auth.jsx'

export default function WaitlistOfferPage() {
  const { token } = useParams()
  const { user } = useAuth()
  const [offer, setOffer] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => { load() }, [token])

  async function load() {
    try { setOffer(await api(`/waitlist/offer/${token}`)) } catch (e) { setMessage(e.message) }
  }

  async function accept() {
    if (!user) { setMessage('Log in with the account that received this offer, then come back to this link.'); return }
    try {
      const booking = await api(`/waitlist/offer/${token}/accept`, { method: 'POST', token: user.token })
      setMessage(`Booked! Reference: ${booking.bookingReference}. Check your email for the QR ticket.`)
    } catch (e) { setMessage(e.message) }
  }

  if (message && !offer) return <p>{message}</p>
  if (!offer) return <p>Loading offer...</p>

  return (
    <div style={{ maxWidth: 420, margin: '40px auto', padding: 20, border: '1px solid #2a2d34', borderRadius: 10 }}>
      <h2>Seat available: {offer.showTitle}</h2>
      <p>Seat {offer.seat} has been reserved for you as the next person on the waitlist.</p>
      <p style={{ color: '#c98a2f' }}>Offer expires at {new Date(offer.expiresAt).toLocaleTimeString()}</p>
      <button onClick={accept}>Accept & Book</button>
      {message && <p style={{ marginTop: 12 }}>{message}</p>}
    </div>
  )
}
