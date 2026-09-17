import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { useToast } from '../../context/ToastContext'

const STATUS_BADGE = {
  OPEN: 'badge-warning', IN_PROGRESS: 'badge-warning', RESOLVED: 'badge-success', CLOSED: 'badge-neutral',
}

export default function SupportPage() {
  const navigate = useNavigate()
  const showToast = useToast()
  const [tickets, setTickets] = useState(null)
  const [subject, setSubject] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get('/support/tickets').then(setTickets).catch(() => setTickets([]))
  }
  useEffect(load, [])

  async function createTicket() {
    if (!subject.trim() || !description.trim()) return
    setBusy(true)
    setError('')
    try {
      await api.post('/support/tickets', { subject, description })
      setSubject('')
      setDescription('')
      showToast('Ticket created. Our team will respond soon.')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create your ticket.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Support</h1>
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <strong>New ticket</strong>
        <TextField id="subject" label="Subject" value={subject} onChange={(e) => setSubject(e.target.value)} />
        <TextField id="desc" label="Describe your issue" as="textarea" rows={3} value={description} onChange={(e) => setDescription(e.target.value)} />
        {error && <div className="field error">{error}</div>}
        <Button disabled={busy} onClick={createTicket}>Submit</Button>
      </div>

      <div>
        <strong>Your tickets</strong>
        <div className="selection-list" style={{ marginTop: 8 }}>
          {tickets === null ? (
            <div className="skeleton" style={{ height: 60 }} />
          ) : tickets.length === 0 ? (
            <div className="empty-state">No support tickets yet.</div>
          ) : tickets.map((t) => (
            <div key={t.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{t.subject}</strong>
                <span className={`badge ${STATUS_BADGE[t.status] || 'badge-neutral'}`}>{t.status.replace('_', ' ')}</span>
              </div>
              <p className="subtitle" style={{ marginTop: 6 }}>{t.description}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
