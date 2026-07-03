import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../auth.jsx'

const gradients = [
  'linear-gradient(135deg, #e50914 0%, #7c0a12 100%)',
  'linear-gradient(135deg, #2b5876 0%, #4e4376 100%)',
  'linear-gradient(135deg, #ff512f 0%, #dd2476 100%)',
  'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)',
  'linear-gradient(135deg, #833ab4 0%, #fd1d1d 50%, #fcb045 100%)',
  'linear-gradient(135deg, #4776e6 0%, #8e54e9 100%)',
]

function gradientFor(id) {
  return gradients[id % gradients.length]
}

export default function Events() {
  const [shows, setShows] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    load()
  }, [])

  async function load() {
    setLoading(true)
    try {
      const q = search ? `?search=${encodeURIComponent(search)}` : ''
      const data = await api(`/shows${q}`)
      setShows(data)
    } catch (e) { console.error(e) }
    setLoading(false)
  }

  return (
    <div style={{ background: '#0a0a0a', minHeight: '100vh', color: '#fff' }}>
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;600;700&display=swap');
        * { font-family: 'Poppins', sans-serif; box-sizing: border-box; }
        .hero {
          background: linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(10,10,10,1) 90%),
                      linear-gradient(120deg, #e50914 0%, #221f1f 60%);
          padding: 80px 40px 60px;
          text-align: center;
        }
        .hero h1 {
          font-size: 42px;
          font-weight: 700;
          margin: 0 0 10px;
          letter-spacing: -1px;
        }
        .hero p {
          color: #ccc;
          font-size: 16px;
          margin-bottom: 30px;
        }
        .search-bar {
          display: flex;
          justify-content: center;
          gap: 10px;
          max-width: 500px;
          margin: 0 auto;
        }
        .search-bar input {
          flex: 1;
          padding: 14px 18px;
          border-radius: 30px;
          border: none;
          outline: none;
          font-size: 14px;
          background: #1c1c1c;
          color: #fff;
        }
        .search-bar button {
          padding: 14px 26px;
          border-radius: 30px;
          border: none;
          background: #e50914;
          color: #fff;
          font-weight: 600;
          cursor: pointer;
          transition: transform 0.2s, background 0.2s;
        }
        .search-bar button:hover {
          background: #f6121d;
          transform: scale(1.05);
        }
        .section-title {
          font-size: 24px;
          font-weight: 600;
          padding: 30px 40px 10px;
        }
        .card-grid {
          display: grid;
          grid-template-columns: repeat(auto-fill, minmax(230px, 1fr));
          gap: 22px;
          padding: 10px 40px 60px;
        }
        .card {
          position: relative;
          border-radius: 12px;
          overflow: hidden;
          height: 260px;
          text-decoration: none;
          color: #fff;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          padding: 18px;
          transition: transform 0.25s ease, box-shadow 0.25s ease;
          box-shadow: 0 4px 14px rgba(0,0,0,0.4);
        }
        .card:hover {
          transform: translateY(-8px) scale(1.03);
          box-shadow: 0 12px 30px rgba(0,0,0,0.6);
        }
        .card::after {
          content: '';
          position: absolute;
          inset: 0;
          background: linear-gradient(180deg, rgba(0,0,0,0) 30%, rgba(0,0,0,0.85) 100%);
        }
        .card-content {
          position: relative;
          z-index: 1;
        }
        .card-type {
          font-size: 11px;
          text-transform: uppercase;
          letter-spacing: 1px;
          background: rgba(255,255,255,0.15);
          display: inline-block;
          padding: 3px 10px;
          border-radius: 20px;
          margin-bottom: 8px;
          backdrop-filter: blur(4px);
        }
        .card-title {
          font-size: 19px;
          font-weight: 700;
          margin-bottom: 6px;
          line-height: 1.2;
        }
        .card-meta {
          font-size: 12.5px;
          color: #ddd;
        }
        .empty-state {
          text-align: center;
          color: #888;
          padding: 60px 20px;
          font-size: 15px;
        }
      `}</style>

      <div className="hero">
        <h1>🎬 Find Your Next Show</h1>
        <p>Movies, concerts & live events — book your seats in seconds</p>
        <div className="search-bar">
          <input
            placeholder="Search movies/concerts..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && load()}
          />
          <button onClick={load}>Search</button>
        </div>
      </div>

      <div className="section-title">Browse Events</div>

      {loading ? (
        <div className="empty-state">Loading events...</div>
      ) : shows.length === 0 ? (
        <div className="empty-state">No events yet. Check back soon!</div>
      ) : (
        <div className="card-grid">
          {shows.map(s => (
            <Link
              key={s.id}
              to={`/shows/${s.id}`}
              className="card"
              style={{ background: gradientFor(s.id) }}
            >
              <div className="card-content">
                <span className="card-type">{s.type}</span>
                <div className="card-title">{s.title}</div>
                <div className="card-meta">{new Date(s.startTime).toLocaleString()}</div>
                <div className="card-meta">{s.venue?.name}, {s.venue?.city}</div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}