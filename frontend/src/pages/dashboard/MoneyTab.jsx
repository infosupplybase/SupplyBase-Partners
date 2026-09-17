import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { useToast } from '../../context/ToastContext'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
}

const STATUS_BADGE = {
  REQUESTED: 'badge-warning', SCHEDULED: 'badge-warning', PROCESSING: 'badge-warning',
  PAID: 'badge-success', FAILED: 'badge-danger', MANUAL_RECONCILED: 'badge-success',
}

export default function MoneyTab() {
  const showToast = useToast()
  const [summary, setSummary] = useState(null)
  const [ledger, setLedger] = useState(null)
  const [payouts, setPayouts] = useState(null)
  const [amount, setAmount] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get('/money/summary').then(setSummary).catch(() => setSummary(null))
    api.get('/money/ledger').then(setLedger).catch(() => setLedger([]))
    api.get('/money/payouts').then(setPayouts).catch(() => setPayouts([]))
  }
  useEffect(load, [])

  async function requestPayout() {
    const paise = Math.round(Number(amount) * 100)
    if (!paise || paise <= 0) return
    setBusy(true)
    setError('')
    try {
      await api.post('/money/payouts', { amountPaise: paise })
      showToast('Payout requested.')
      setAmount('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not request a payout.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <h2 className="title">Money</h2>

      {summary === null ? (
        <div className="skeleton" style={{ height: 120 }} />
      ) : (
        <div className="card" style={{ background: 'var(--color-accent-soft)', border: 'none' }}>
          <div className="badge badge-neutral" style={{ marginBottom: 6 }}>This month, Asia/Kolkata</div>
          <div style={{ fontSize: 30, fontWeight: 800, color: 'var(--color-accent-dark)' }}>{formatPaise(summary.availablePaise)}</div>
          <p className="subtitle">available for payout</p>
          <div style={{ display: 'flex', gap: 16, marginTop: 12, fontSize: 13 }}>
            <span>Earned: <strong>{formatPaise(summary.earnedPaise)}</strong></span>
            <span>Scheduled: <strong>{formatPaise(summary.scheduledPaise)}</strong></span>
            <span>Paid: <strong>{formatPaise(summary.paidPaise)}</strong></span>
          </div>
        </div>
      )}

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <strong>Request a payout</strong>
        <TextField id="amount" label="Amount (₹)" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} />
        {error && <div className="field error">{error}</div>}
        <Button disabled={busy || !amount} onClick={requestPayout}>{busy ? 'Requesting…' : 'Request payout'}</Button>
      </div>

      <div>
        <strong>Recent transfers</strong>
        <div className="selection-list" style={{ marginTop: 8 }}>
          {payouts === null ? (
            <div className="skeleton" style={{ height: 60 }} />
          ) : payouts.length === 0 ? (
            <div className="empty-state">No payouts yet.</div>
          ) : payouts.map((p) => (
            <div key={p.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{formatPaise(p.amountPaise)}</span>
              <span className={`badge ${STATUS_BADGE[p.status] || 'badge-neutral'}`}>{p.status.replace('_', ' ')}</span>
            </div>
          ))}
        </div>
      </div>

      <div>
        <strong>Earnings history</strong>
        <div className="selection-list" style={{ marginTop: 8 }}>
          {ledger === null ? (
            <div className="skeleton" style={{ height: 60 }} />
          ) : ledger.length === 0 ? (
            <div className="empty-state">No completed jobs yet.</div>
          ) : ledger.map((e) => (
            <div key={e.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Job #{e.serviceRequestId}</span>
              <span style={{ fontWeight: 700 }}>{e.status === 'REVERSED' ? '—' : formatPaise(e.netPaise)}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
