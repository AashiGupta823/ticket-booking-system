import React, { useState } from 'react'
import { api, useAuth } from '../auth.jsx'

// Minimal admin form: define categories, then rows of seats mapped to a category.
export default function CreateVenue() {
  const { user } = useAuth()
  const [name, setName] = useState('')
  const [address, setAddress] = useState('')
  const [city, setCity] = useState('')
  const [categories, setCategories] = useState('Premium,Standard')
  const [rows, setRows] = useState([{ rowLabel: 'A', seatCount: 10, categoryName: 'Premium' }])
  const [message, setMessage] = useState('')

  function addRow() {
    setRows([...rows, { rowLabel: '', seatCount: 10, categoryName: '' }])
  }
  function updateRow(i, field, value) {
    const copy = [...rows]
    copy[i][field] = field === 'seatCount' ? Number(value) : value
    setRows(copy)
  }

  async function submit(e) {
    e.preventDefault()
    try {
      const body = {
        name, address, city,
        categories: categories.split(',').map(c => ({ name: c.trim() })).filter(c => c.name),
        rows,
      }
      const venue = await api('/venues', { method: 'POST', body, token: user.token })
      setMessage(`Venue created: ${venue.name} (id ${venue.id})`)
    } catch (e) { setMessage(e.message) }
  }

  if (!user || user.role !== 'ADMIN') return <p>Admin access only.</p>

  return (
    <div style={{ maxWidth: 520 }}>
      <h2>Create Venue</h2>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <input placeholder="Venue name" value={name} onChange={e => setName(e.target.value)} />
        <input placeholder="Address" value={address} onChange={e => setAddress(e.target.value)} />
        <input placeholder="City" value={city} onChange={e => setCity(e.target.value)} />
        <input placeholder="Categories (comma-separated)" value={categories} onChange={e => setCategories(e.target.value)} />
        <h4>Seat Rows</h4>
        {rows.map((r, i) => (
          <div key={i} style={{ display: 'flex', gap: 8 }}>
            <input placeholder="Row (e.g. A)" value={r.rowLabel} onChange={e => updateRow(i, 'rowLabel', e.target.value)} style={{ width: 60 }} />
            <input placeholder="Seat count" type="number" value={r.seatCount} onChange={e => updateRow(i, 'seatCount', e.target.value)} style={{ width: 90 }} />
            <input placeholder="Category" value={r.categoryName} onChange={e => updateRow(i, 'categoryName', e.target.value)} />
          </div>
        ))}
        <button type="button" onClick={addRow}>+ Add Row</button>
        <button type="submit">Create Venue</button>
      </form>
      {message && <p>{message}</p>}
    </div>
  )
}
