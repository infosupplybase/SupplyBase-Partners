import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import { useToast } from '../../context/ToastContext'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN')}`
}

export default function JobDetailPage() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const showToast = useToast()
  const [job, setJob] = useState(null)
  const [files, setFiles] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function load() {
    api.get(`/jobs/${jobId}`).then(setJob).catch(() => setJob(null))
  }
  useEffect(load, [jobId])

  async function start() {
    setBusy(true)
    setError('')
    try {
      await api.post(`/jobs/${jobId}/start`)
      showToast('Job started.')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not start this job.')
    } finally {
      setBusy(false)
    }
  }

  async function submitCompletion() {
    setBusy(true)
    setError('')
    try {
      const form = new FormData()
      files.forEach((f) => form.append('files', f))
      await api.postForm(`/jobs/${jobId}/completion`, form)
      showToast('Submitted for confirmation.')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not submit completion.')
    } finally {
      setBusy(false)
    }
  }

  if (!job) {
    return <div className="screen"><div className="skeleton" style={{ height: 200 }} /></div>
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/around-you')} aria-label="Back">←</button>
        <h1>Job #{job.id}</h1>
      </div>

      <span className="badge badge-warning" style={{ alignSelf: 'flex-start' }}>{job.status.replace('_', ' ')}</span>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
          <strong>Your earning</strong><strong>{formatPaise(job.partnerEarningPaise)}</strong>
        </div>
        <p className="subtitle" style={{ marginTop: 8 }}>{new Date(job.scheduledAt).toLocaleString('en-IN', { dateStyle: 'full', timeStyle: 'short' })}</p>
        {job.notes && <p className="subtitle">{job.notes}</p>}
      </div>

      <div className="card">
        <strong>Customer</strong>
        <p className="subtitle" style={{ marginTop: 6 }}>{job.customerName}</p>
        <p className="subtitle">{job.customerPhone}</p>
        <p className="subtitle">{job.addressLine1}{job.addressLine2 ? `, ${job.addressLine2}` : ''}</p>
      </div>

      {job.status === 'ASSIGNED' && (
        <Button block disabled={busy} onClick={start}>{busy ? 'Starting…' : 'Start job'}</Button>
      )}

      {job.status === 'IN_PROGRESS' && (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          <strong>Submit completion</strong>
          <input type="file" multiple accept="image/*" onChange={(e) => setFiles(Array.from(e.target.files || []))} />
          <Button disabled={busy} onClick={submitCompletion}>{busy ? 'Submitting…' : 'Submit for confirmation'}</Button>
        </div>
      )}

      {job.status === 'COMPLETION_SUBMITTED' && (
        <div className="card empty-state">Waiting for admin/customer confirmation.</div>
      )}

      {error && <div className="field error">{error}</div>}
    </div>
  )
}
