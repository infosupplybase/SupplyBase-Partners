import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

export default function AdminCommunity() {
  const [reports, setReports] = useState(null)
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')

  function load() {
    api.get('/admin/community/reports').then(setReports).catch(() => setReports([]))
  }
  useEffect(load, [])

  async function publish() {
    if (!title.trim() || !body.trim()) return
    await api.post('/admin/community/announcements', { title, body }).catch(() => {})
    setTitle('')
    setBody('')
  }

  async function moderate(postId, hide) {
    await api.post(`/admin/community/posts/${postId}/moderate`, { hide }).catch(() => {})
    load()
  }

  async function resolveReport(reportId, dismiss) {
    await api.post(`/admin/community/reports/${reportId}/resolve`, { dismiss }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Community</h2>
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 16 }}>
        <strong>Publish announcement</strong>
        <TextField id="title" label="Title" value={title} onChange={(e) => setTitle(e.target.value)} />
        <TextField id="body" label="Body" as="textarea" rows={3} value={body} onChange={(e) => setBody(e.target.value)} />
        <Button onClick={publish}>Publish</Button>
      </div>

      <h3 style={{ marginTop: 24 }}>Open reports</h3>
      {reports === null ? (
        <div className="skeleton" style={{ height: 100 }} />
      ) : reports.length === 0 ? (
        <div className="empty-state">No open reports.</div>
      ) : (
        <div className="selection-list">
          {reports.map((r) => (
            <div key={r.id} className="card">
              <p style={{ margin: 0 }}>{r.targetType} #{r.targetId} &mdash; {r.reason}</p>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <Button variant="danger" onClick={() => moderate(r.targetId, true)}>Hide content</Button>
                <Button variant="secondary" onClick={() => resolveReport(r.id, true)}>Dismiss report</Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
