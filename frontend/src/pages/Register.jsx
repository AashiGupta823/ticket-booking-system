import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, useAuth } from '../auth.jsx'

export default function Register() {
  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'CUSTOMER' })
  const [error, setError] = useState('')
  const { login } = useAuth()
  const nav = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await api('/auth/register', { method: 'POST', body: form })
      login(res)
      nav('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div style={{ maxWidth: 360, margin: '40px auto' }}>
      <h2>Register</h2>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <input placeholder="Name" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        <input placeholder="Email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} />
        <input placeholder="Password" type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} />
        <select value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>
          <option value="CUSTOMER">Customer</option>
          <option value="ORGANISER">Organiser</option>
          <option value="ADMIN">Admin</option>
        </select>
        <button type="submit">Register</button>
      </form>
      {error && <p style={{ color: '#ff6b6b' }}>{error}</p>}
    </div>
  )
}
