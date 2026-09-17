import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import SelectionCard from '../../components/SelectionCard'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

function formatDate(iso) {
  return new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
}

export default function ScreeningPage() {
  const navigate = useNavigate()
  const { partner } = usePartner()
  const showToast = useToast()
  const [slots, setSlots] = useState(null)
  const [bookings, setBookings] = useState(null)
  const [selectedSlot, setSelectedSlot] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function loadBookings() {
    api.get('/screening/bookings').then(setBookings).catch(() => setBookings([]))
  }

  useEffect(() => {
    if (!partner?.primaryCategoryId || !partner?.residenceCityId) return
    api.get(`/screening/slots?categoryId=${partner.primaryCategoryId}&cityId=${partner.residenceCityId}`)
      .then(setSlots).catch(() => setSlots([]))
    loadBookings()
  }, [partner])

  const activeBooking = bookings?.find((b) => b.status === 'BOOKED' || b.status === 'CHECKED_IN')

  async function book() {
    if (!selectedSlot) return
    setBusy(true)
    setError('')
    try {
      await api.post('/screening/bookings', { slotId: selectedSlot })
      showToast('Session booked.')
      loadBookings()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not book this slot.')
    } finally {
      setBusy(false)
    }
  }

  async function checkIn(bookingId) {
    setBusy(true)
    setError('')
    try {
      await api.post(`/screening/bookings/${bookingId}/check-in`)
      showToast('Checked in.')
      loadBookings()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Check-in is not available yet.')
    } finally {
      setBusy(false)
    }
  }

  async function cancel(bookingId) {
    setBusy(true)
    setError('')
    try {
      await api.post(`/screening/bookings/${bookingId}/cancel`)
      showToast('Booking cancelled.')
      loadBookings()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not cancel this booking.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Skill session</h1>
      </div>

      <div className="card">
        <strong>What is a skill session?</strong>
        <p className="subtitle" style={{ marginTop: 4 }}>
          A short in-person or virtual check where our team confirms you're ready to take jobs safely and professionally.
        </p>
        <strong style={{ display: 'block', marginTop: 10 }}>How to prepare?</strong>
        <p className="subtitle" style={{ marginTop: 4 }}>Bring your tools/uniform if in-person, and arrive a few minutes early.</p>
        <strong style={{ display: 'block', marginTop: 10 }}>How to attend?</strong>
        <p className="subtitle" style={{ marginTop: 4 }}>Use the venue/directions or virtual link shown on your booking below.</p>
      </div>

      {activeBooking ? (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <strong>Your booking</strong>
            <span className="badge badge-warning">{activeBooking.status.replace('_', ' ')}</span>
          </div>
          <p className="subtitle" style={{ marginTop: 6 }}>Slot #{activeBooking.slotId}</p>
          {error && <div className="field error">{error}</div>}
          <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
            {activeBooking.status === 'BOOKED' && (
              <Button disabled={busy} onClick={() => checkIn(activeBooking.id)}>Check in</Button>
            )}
            <Button variant="danger" disabled={busy} onClick={() => cancel(activeBooking.id)}>Cancel</Button>
          </div>
        </div>
      ) : (
        <>
          <p className="subtitle">Choose an available slot:</p>
          {slots === null ? (
            <div className="skeleton" style={{ height: 100 }} />
          ) : slots.length === 0 ? (
            <div className="empty-state">No upcoming slots for your category/city right now. Check back soon.</div>
          ) : (
            <div className="selection-list">
              {slots.map((s) => (
                <SelectionCard
                  key={s.id} selected={selectedSlot === s.id} icon={s.mode === 'IN_PERSON' ? '📍' : '💻'}
                  label={formatDate(s.startsAt)}
                  meta={`${s.mode === 'IN_PERSON' ? s.venueOrLink : 'Virtual session'} · ${s.remainingCapacity} seats left`}
                  onClick={() => setSelectedSlot(s.id)}
                />
              ))}
            </div>
          )}
          {error && <div className="field error">{error}</div>}
          <Button block disabled={!selectedSlot || busy} onClick={book}>{busy ? 'Booking…' : 'Book session'}</Button>
        </>
      )}
    </div>
  )
}
