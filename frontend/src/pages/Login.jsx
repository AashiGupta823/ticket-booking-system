import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api, useAuth } from '../auth.jsx'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const { login } = useAuth()
  const nav = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    try {
      const res = await api('/auth/login', { method: 'POST', body: { email, password } })
      login(res)
      nav('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div style={{ maxWidth: 360, margin: '40px auto' }}>
      <h2>Login</h2>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <input placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} />
        <input placeholder="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} />
        <button type="submit">Login</button>
      </form>
      {error && <p style={{ color: '#ff6b6b' }}>{error}</p>}
      <p>No account? <Link to="/register">Register</Link></p>
    </div>
  )
}
