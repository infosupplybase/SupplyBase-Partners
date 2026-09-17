import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

function toLocalInputValue(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export default function AdminScreening() {
  const [categories, setCategories] = useState([])
  const [cities, setCities] = useState([])
  const [slots, setSlots] = useState(null)
  const [checkedIn, setCheckedIn] = useState(null)
  const [error, setError] = useState('')

  const [categoryId, setCategoryId] = useState('')
  const [cityId, setCityId] = useState('')
  const [mode, setMode] = useState('VIRTUAL')
  const [venueOrLink, setVenueOrLink] = useState('')
  const [startsAt, setStartsAt] = useState(toLocalInputValue(new Date(Date.now() + 3 * 86400000)))
  const [capacity, setCapacity] = useState(10)

  function load() {
    api.get('/admin/screening/slots').then(setSlots).catch(() => setSlots([]))
    api.get('/admin/screening/bookings/checked-in').then(setCheckedIn).catch(() => setCheckedIn([]))
  }

  useEffect(() => {
    api.get('/catalog/categories').then(setCategories).catch(() => {})
    api.get('/catalog/cities').then(setCities).catch(() => {})
    load()
  }, [])

  async function createSlot() {
    if (!categoryId || !cityId || !venueOrLink.trim()) return
    setError('')
    const starts = new Date(startsAt)
    const ends = new Date(starts.getTime() + 60 * 60 * 1000)
    const checkInOpens = new Date(starts.getTime() - 15 * 60 * 1000)
    const checkInCloses = new Date(starts.getTime() + 15 * 60 * 1000)
    try {
      await api.post('/admin/screening/slots', {
        categoryId: Number(categoryId), cityId: Number(cityId), mode, venueOrLink,
        startsAt: starts.toISOString(), endsAt: ends.toISOString(),
        checkInOpensAt: checkInOpens.toISOString(), checkInClosesAt: checkInCloses.toISOString(),
        capacity: Number(capacity),
      })
      setVenueOrLink('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create slot.')
    }
  }

  async function decide(bookingId, passed) {
    const reason = passed ? 'Confident and prepared' : window.prompt('Reason for not passing?') || ''
    if (!passed && !reason) return
    await api.post(`/admin/screening/bookings/${bookingId}/decision`, { passed, reason }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Screening</h2>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 16, maxWidth: 480 }}>
        <strong>Create a slot</strong>
        <div className="field">
          <label>Category</label>
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Select…</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>City</label>
          <select value={cityId} onChange={(e) => setCityId(e.target.value)}>
            <option value="">Select…</option>
            {cities.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Mode</label>
          <select value={mode} onChange={(e) => setMode(e.target.value)}>
            <option value="VIRTUAL">Virtual</option>
            <option value="IN_PERSON">In person</option>
          </select>
        </div>
        <TextField id="venue" label={mode === 'VIRTUAL' ? 'Meeting link' : 'Venue address'} value={venueOrLink} onChange={(e) => setVenueOrLink(e.target.value)} />
        <TextField id="startsAt" label="Starts at" type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)} />
        <TextField id="capacity" label="Capacity" type="number" min="1" value={capacity} onChange={(e) => setCapacity(e.target.value)} />
        {error && <div className="field error">{error}</div>}
        <Button onClick={createSlot}>Create slot</Button>
      </div>

      <h3 style={{ marginTop: 24 }}>Upcoming slots</h3>
      <table className="data-table">
        <thead><tr><th>Starts</th><th>Mode</th><th>Venue</th><th>Booked / Capacity</th></tr></thead>
        <tbody>
          {(slots || []).map((s) => (
            <tr key={s.id}>
              <td>{new Date(s.startsAt).toLocaleString('en-IN')}</td>
              <td>{s.mode}</td>
              <td>{s.venueOrLink}</td>
              <td>{s.remainingCapacity != null ? `${s.remainingCapacity} left` : '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 style={{ marginTop: 24 }}>Checked in, awaiting outcome</h3>
      {checkedIn === null ? (
        <div className="skeleton" style={{ height: 80 }} />
      ) : checkedIn.length === 0 ? (
        <div className="empty-state">Nobody checked in yet.</div>
      ) : (
        <div className="selection-list">
          {checkedIn.map((b) => (
            <div key={b.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>Booking #{b.id} (slot #{b.slotId})</span>
              <div style={{ display: 'flex', gap: 8 }}>
                <Button onClick={() => decide(b.id, true)}>Pass</Button>
                <Button variant="danger" onClick={() => decide(b.id, false)}>Fail</Button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
