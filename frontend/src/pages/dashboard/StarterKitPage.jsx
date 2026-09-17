import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
}

const STATUS_BADGE = {
  QUOTE: 'badge-neutral', PENDING_PAYMENT: 'badge-warning', PAID: 'badge-warning',
  PROCESSING: 'badge-warning', SHIPPED: 'badge-warning', DELIVERED: 'badge-success',
  CANCELLED: 'badge-danger', REFUNDED: 'badge-danger', PAYMENT_FAILED: 'badge-danger',
}

export default function StarterKitPage() {
  const navigate = useNavigate()
  const { partner, refresh } = usePartner()
  const showToast = useToast()
  const [quote, setQuote] = useState(undefined)
  const [orders, setOrders] = useState(null)
  const [address, setAddress] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get('/kit-orders/quote').then(setQuote).catch(() => setQuote({ available: false }))
    api.get('/kit-orders').then(setOrders).catch(() => setOrders([]))
  }
  useEffect(load, [])

  const activeOrder = orders?.[0]

  async function bookKit() {
    if (!address.trim()) return
    setBusy(true)
    setError('')
    try {
      await api.post('/kit-orders', {
        deliveryAddressLine: address,
        deliveryCityId: partner.residenceCityId,
        idempotencyKey: crypto.randomUUID(),
      })
      showToast('Starter kit booked.')
      load()
      await refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not book your starter kit.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Starter kit</h1>
      </div>

      {quote === undefined || orders === null ? (
        <div className="skeleton" style={{ height: 160 }} />
      ) : activeOrder ? (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <strong>Order #{activeOrder.id}</strong>
            <span className={`badge ${STATUS_BADGE[activeOrder.status] || 'badge-neutral'}`}>{activeOrder.status.replace('_', ' ')}</span>
          </div>
          <p className="subtitle" style={{ marginTop: 8 }}>Delivering to: {activeOrder.deliveryAddressLine}</p>
          <p className="subtitle">Total paid: {formatPaise(activeOrder.totalPaise)}</p>
        </div>
      ) : quote.available ? (
        <>
          <div className="card">
            <strong>{quote.name}</strong>
            <p className="subtitle" style={{ marginTop: 4 }}>{quote.description}</p>
            <ul style={{ margin: '10px 0', paddingLeft: 20 }}>
              {quote.items.map((item) => <li key={item}>{item}</li>)}
            </ul>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
              <span>Kit price</span><span>{formatPaise(quote.pricePaise)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 14 }}>
              <span>Delivery fee</span><span>{formatPaise(quote.feesPaise)}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 700, marginTop: 6 }}>
              <span>Total</span><span>{formatPaise(quote.totalPaise)}</span>
            </div>
            <p className="subtitle" style={{ marginTop: 10, fontSize: 12 }}>{quote.termsText}</p>
          </div>
          <TextField
            id="address" label="Delivery address" as="textarea" rows={3}
            value={address} onChange={(e) => setAddress(e.target.value)} required
          />
          {error && <div className="field error">{error}</div>}
          <Button block disabled={!address.trim() || busy} onClick={bookKit}>
            {busy ? 'Processing…' : `Book Starter Kit — ${formatPaise(quote.totalPaise)}`}
          </Button>
        </>
      ) : (
        <div className="card empty-state">No starter kit is required for your category &mdash; you can continue.</div>
      )}
    </div>
  )
}
