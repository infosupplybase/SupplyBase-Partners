import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api/client'

export default function NotificationsPage() {
  const navigate = useNavigate()
  const [items, setItems] = useState(null)

  useEffect(() => {
    api.get('/notifications').then(setItems).catch(() => setItems([]))
  }, [])

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Notifications</h1>
      </div>
      <div className="selection-list">
        {items === null ? (
          <div className="skeleton" style={{ height: 60 }} />
        ) : items.length === 0 ? (
          <div className="empty-state">No notifications yet.</div>
        ) : items.map((n) => (
          <div key={n.id} className="card">
            <strong style={{ textTransform: 'capitalize' }}>{n.templateKey.replaceAll('.', ' ').replaceAll('_', ' ')}</strong>
            <p className="subtitle" style={{ marginTop: 4, fontSize: 13 }}>{n.payloadJson}</p>
            <p className="subtitle" style={{ fontSize: 11 }}>{new Date(n.createdAt).toLocaleString('en-IN')}</p>
          </div>
        ))}
      </div>
    </div>
  )
}
