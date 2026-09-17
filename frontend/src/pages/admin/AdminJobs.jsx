import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN')}`
}

export default function AdminJobs() {
  const [requests, setRequests] = useState(null)
  const [categories, setCategories] = useState([])
  const [services, setServices] = useState([])
  const [cities, setCities] = useState([])
  const [areas, setAreas] = useState([])
  const [error, setError] = useState('')

  const [customerName, setCustomerName] = useState('')
  const [customerPhone, setCustomerPhone] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [serviceId, setServiceId] = useState('')
  const [cityId, setCityId] = useState('')
  const [areaId, setAreaId] = useState('')
  const [line1, setLine1] = useState('')
  const [scheduledAt, setScheduledAt] = useState('')
  const [jobValue, setJobValue] = useState('1000')
  const [partnerEarning, setPartnerEarning] = useState('700')

  function load() {
    api.get('/admin/jobs/service-requests').then(setRequests).catch(() => setRequests([]))
  }

  useEffect(() => {
    api.get('/catalog/categories').then(setCategories).catch(() => {})
    api.get('/catalog/cities').then(setCities).catch(() => {})
    load()
  }, [])

  useEffect(() => {
    if (categoryId) api.get(`/catalog/categories/${categoryId}/services`).then(setServices).catch(() => setServices([]))
  }, [categoryId])

  useEffect(() => {
    if (cityId) api.get(`/catalog/cities/${cityId}/areas`).then(setAreas).catch(() => setAreas([]))
  }, [cityId])

  async function createJob() {
    setError('')
    try {
      const customer = await api.post('/admin/jobs/customers', { name: customerName, phone: customerPhone });
      const address = await api.post(`/admin/jobs/customers/${customer.id}/addresses`, { line1, areaId: Number(areaId) });
      await api.post('/admin/jobs/service-requests', {
        customerId: customer.id, serviceId: Number(serviceId), addressId: address.id,
        scheduledAt: new Date(scheduledAt).toISOString(), notes: '',
        jobValuePaise: Math.round(Number(jobValue) * 100), partnerEarningPaise: Math.round(Number(partnerEarning) * 100),
      })
      setCustomerName(''); setCustomerPhone(''); setLine1('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create this job.')
    }
  }

  async function confirmCompletion(id) {
    await api.post(`/admin/jobs/service-requests/${id}/confirm-completion`).catch(() => {})
    load()
  }

  async function cancel(id) {
    const reason = window.prompt('Cancellation reason?')
    if (!reason) return
    await api.post(`/admin/jobs/service-requests/${id}/cancel`, { reason }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Jobs</h2>
      <p className="subtitle">No real customer app exists in this build — create synthetic jobs here.</p>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 16, maxWidth: 480 }}>
        <strong>New job</strong>
        <TextField id="custName" label="Customer name" value={customerName} onChange={(e) => setCustomerName(e.target.value)} />
        <TextField id="custPhone" label="Customer phone" value={customerPhone} onChange={(e) => setCustomerPhone(e.target.value)} placeholder="9876543210" />
        <div className="field">
          <label>Category</label>
          <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Select…</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Service</label>
          <select value={serviceId} onChange={(e) => setServiceId(e.target.value)}>
            <option value="">Select…</option>
            {services.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
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
          <label>Area</label>
          <select value={areaId} onChange={(e) => setAreaId(e.target.value)}>
            <option value="">Select…</option>
            {areas.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>
        <TextField id="line1" label="Address" value={line1} onChange={(e) => setLine1(e.target.value)} />
        <TextField id="scheduledAt" label="Scheduled at" type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} />
        <TextField id="jobValue" label="Customer job value (₹)" type="number" value={jobValue} onChange={(e) => setJobValue(e.target.value)} />
        <TextField id="partnerEarning" label="Partner earning (₹)" type="number" value={partnerEarning} onChange={(e) => setPartnerEarning(e.target.value)} />
        {error && <div className="field error">{error}</div>}
        <Button disabled={!customerName || !customerPhone || !serviceId || !areaId || !line1 || !scheduledAt} onClick={createJob}>Create job</Button>
      </div>

      <h3 style={{ marginTop: 24 }}>All jobs</h3>
      {requests === null ? (
        <div className="skeleton" style={{ height: 150 }} />
      ) : (
        <table className="data-table">
          <thead><tr><th>ID</th><th>Status</th><th>Value</th><th>Earning</th><th>Actions</th></tr></thead>
          <tbody>
            {requests.map((r) => (
              <tr key={r.id}>
                <td>{r.id}</td>
                <td>{r.status.replace('_', ' ')}</td>
                <td>{formatPaise(r.jobValuePaise)}</td>
                <td>{formatPaise(r.partnerEarningPaise)}</td>
                <td style={{ display: 'flex', gap: 8 }}>
                  {r.status === 'COMPLETION_SUBMITTED' && <Button onClick={() => confirmCompletion(r.id)}>Confirm</Button>}
                  {r.status !== 'COMPLETED' && r.status !== 'CANCELLED' && <Button variant="danger" onClick={() => cancel(r.id)}>Cancel</Button>}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
