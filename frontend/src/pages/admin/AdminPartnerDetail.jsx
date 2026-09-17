import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'

export default function AdminPartnerDetail() {
  const { partnerId } = useParams()
  const navigate = useNavigate()
  const [partner, setPartner] = useState(null)
  const [eligibility, setEligibility] = useState(null)
  const [history, setHistory] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get(`/admin/partners/${partnerId}`).then(setPartner).catch(() => setPartner(null))
    api.get(`/admin/partners/${partnerId}/activation-eligibility`).then(setEligibility).catch(() => setEligibility(null))
    api.get(`/admin/partners/${partnerId}/status-history`).then(setHistory).catch(() => setHistory([]))
  }
  useEffect(load, [partnerId])

  async function activate() {
    setBusy(true)
    setError('')
    try {
      await api.post(`/admin/partners/${partnerId}/activate`)
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not activate this partner.')
    } finally {
      setBusy(false)
    }
  }

  async function suspend() {
    const reason = window.prompt('Reason for suspension?')
    if (!reason) return
    setBusy(true)
    setError('')
    try {
      await api.post(`/admin/partners/${partnerId}/suspend`, { reason })
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not suspend this partner.')
    } finally {
      setBusy(false)
    }
  }

  async function reactivate() {
    setBusy(true)
    setError('')
    try {
      await api.post(`/admin/partners/${partnerId}/reactivate`, { reason: 'Reactivated by admin' })
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reactivate this partner.')
    } finally {
      setBusy(false)
    }
  }

  if (!partner) return <div className="skeleton" style={{ height: 200 }} />

  return (
    <div>
      <button className="btn btn-ghost" onClick={() => navigate('/admin/partners')} style={{ paddingLeft: 0 }}>← Back to partners</button>
      <h2 className="title">Partner #{partner.id}</h2>
      <p className="subtitle">Stage: {partner.onboardingStage} &middot; Status: {partner.activationStatus}</p>

      {eligibility && (
        <div className="card" style={{ marginTop: 12 }}>
          <strong>Activation eligibility</strong>
          {eligibility.eligible ? (
            <p className="subtitle" style={{ marginTop: 6 }}>✅ All requirements met.</p>
          ) : (
            <ul style={{ marginTop: 6 }}>
              {eligibility.unmetRequirements.map((r) => <li key={r} className="subtitle">{r}</li>)}
            </ul>
          )}
        </div>
      )}

      {error && <div className="field error" style={{ marginTop: 10 }}>{error}</div>}

      <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
        {partner.activationStatus === 'NOT_ACTIVE' && (
          <Button disabled={busy || !eligibility?.eligible} onClick={activate}>Activate</Button>
        )}
        {partner.activationStatus === 'ACTIVE' && (
          <Button variant="danger" disabled={busy} onClick={suspend}>Suspend</Button>
        )}
        {partner.activationStatus === 'SUSPENDED' && (
          <Button disabled={busy} onClick={reactivate}>Reactivate</Button>
        )}
      </div>

      <h3 style={{ marginTop: 24 }}>Status history</h3>
      <table className="data-table">
        <thead><tr><th>From</th><th>To</th><th>Reason</th><th>When</th></tr></thead>
        <tbody>
          {history.map((h) => (
            <tr key={h.id}>
              <td>{h.fromStatus || '—'}</td><td>{h.toStatus}</td><td>{h.reason || '—'}</td>
              <td>{new Date(h.createdAt).toLocaleString('en-IN')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
