import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Button from '../../components/Button'

export default function AdminSupport() {
  const [page, setPage] = useState(null)
  const [replyDrafts, setReplyDrafts] = useState({})

  function load() {
    api.get('/admin/support/tickets?status=OPEN&page=0&size=50').then(setPage).catch(() => setPage(null))
  }
  useEffect(load, [])

  async function reply(ticketId) {
    const body = replyDrafts[ticketId]
    if (!body?.trim()) return
    await api.post(`/admin/support/tickets/${ticketId}/messages`, { body }).catch(() => {})
    setReplyDrafts((prev) => ({ ...prev, [ticketId]: '' }))
  }

  async function resolve(ticketId) {
    await api.patch(`/admin/support/tickets/${ticketId}/status`, { status: 'RESOLVED' }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Open support tickets</h2>
      {!page ? (
        <div className="skeleton" style={{ height: 200, marginTop: 16 }} />
      ) : page.content.length === 0 ? (
        <div className="empty-state">No open tickets.</div>
      ) : (
        <div className="selection-list" style={{ marginTop: 16 }}>
          {page.content.map((t) => (
            <div key={t.id} className="card">
              <strong>{t.subject}</strong>
              <p className="subtitle" style={{ marginTop: 4 }}>{t.description}</p>
              <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
                <input
                  style={{ flex: 1, border: '1px solid var(--color-border)', borderRadius: 8, padding: 8 }}
                  value={replyDrafts[t.id] || ''} placeholder="Reply…"
                  onChange={(e) => setReplyDrafts((prev) => ({ ...prev, [t.id]: e.target.value }))}
                />
                <Button variant="secondary" onClick={() => reply(t.id)}>Reply</Button>
                <Button variant="danger" onClick={() => resolve(t.id)}>Resolve</Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
