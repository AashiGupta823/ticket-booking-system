import React, { useEffect, useState } from 'react'
import { api, useAuth } from '../auth.jsx'

export default function CreateShow() {
  const { user } = useAuth()
  const [venues, setVenues] = useState([])
  const [title, setTitle] = useState('')
  const [type, setType] = useState('MOVIE')
  const [venueId, setVenueId] = useState('')
  const [startTime, setStartTime] = useState('')
  const [prices, setPrices] = useState('Premium:300,Standard:150')
  const [message, setMessage] = useState('')

  useEffect(() => { api('/venues').then(setVenues).catch(() => {}) }, [])

  async function submit(e) {
    e.preventDefault()
    try {
      const priceInputs = prices.split(',').map(p => {
        const [categoryName, price] = p.split(':')
        return { categoryName: categoryName.trim(), price: Number(price) }
      })
      const body = {
        title, type, venueId: Number(venueId),
        startTime: new Date(startTime).toISOString(),
        description: '',
        prices: priceInputs,
      }
      const show = await api('/shows', { method: 'POST', body, token: user.token })
      setMessage(`Show created: ${show.title} (id ${show.id})`)
    } catch (e) { setMessage(e.message) }
  }

  if (!user || (user.role !== 'ORGANISER' && user.role !== 'ADMIN')) return <p>Organiser access only.</p>

  return (
    <div style={{ maxWidth: 460 }}>
      <h2>Create Show</h2>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <input placeholder="Title" value={title} onChange={e => setTitle(e.target.value)} />
        <select value={type} onChange={e => setType(e.target.value)}>
          <option value="MOVIE">Movie</option>
          <option value="CONCERT">Concert</option>
        </select>
        <select value={venueId} onChange={e => setVenueId(e.target.value)}>
          <option value="">Select venue</option>
          {venues.map(v => <option key={v.id} value={v.id}>{v.name} ({v.city})</option>)}
        </select>
        <input type="datetime-local" value={startTime} onChange={e => setStartTime(e.target.value)} />
        <input placeholder="Prices e.g. Premium:300,Standard:150" value={prices} onChange={e => setPrices(e.target.value)} />
        <button type="submit">Create Show</button>
      </form>
      {message && <p>{message}</p>}
    </div>
  )
}
