import React, { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('tbs_user')
    return raw ? JSON.parse(raw) : null
  })

  function login(authResponse) {
    const u = {
      token: authResponse.token,
      userId: authResponse.userId,
      name: authResponse.name,
      email: authResponse.email,
      role: authResponse.role,
    }
    localStorage.setItem('tbs_user', JSON.stringify(u))
    setUser(u)
  }

  function logout() {
    localStorage.removeItem('tbs_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function api(path, { method = 'GET', body, token } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  const data = text ? JSON.parse(text) : null
  if (!res.ok) {
    throw new Error(data?.message || `Request failed (${res.status})`)
  }
  return data
}
