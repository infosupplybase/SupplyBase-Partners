import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN')}`
}

export default function AroundYouTab() {
  const { partner } = usePartner()
  const showToast = useToast()
  const [jobs, setJobs] = useState(null)
  const [assigned, setAssigned] = useState(null)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  function load() {
    api.get('/jobs/nearby').then(setJobs).catch(() => setJobs([]))
    api.get('/jobs/assigned').then(setAssigned).catch(() => setAssigned([]))
  }
  useEffect(load, [])

  async function accept(jobId) {
    setBusyId(jobId)
    setError('')
    try {
      await api.post(`/jobs/${jobId}/accept`)
      showToast('Job accepted!')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'This job was already taken.')
      load()
    } finally {
      setBusyId(null)
    }
  }

  if (partner && partner.activationStatus !== 'ACTIVE') {
    return (
      <div className="screen">
        <h2 className="title">Around You</h2>
        <div className="card empty-state">
          Finish onboarding and get activated to start seeing and accepting nearby jobs.
          <div style={{ marginTop: 12 }}>
            <Link to="/app/progress"><Button>Go to Progress</Button></Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="screen">
      <h2 className="title">Around You</h2>

      {assigned && assigned.filter((j) => j.status !== 'COMPLETED' && j.status !== 'CANCELLED').length > 0 && (
        <div>
          <strong>Your active jobs</strong>
          <div className="selection-list" style={{ marginTop: 8 }}>
            {assigned.filter((j) => j.status !== 'COMPLETED' && j.status !== 'CANCELLED').map((j) => (
              <Link key={j.id} to={`/app/jobs/${j.id}`} className="card" style={{ textDecoration: 'none', color: 'inherit', display: 'block' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <strong>Job #{j.id}</strong>
                  <span className="badge badge-warning">{j.status.replace('_', ' ')}</span>
                </div>
                <p className="subtitle">{formatPaise(j.partnerEarningPaise)} &middot; {new Date(j.scheduledAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p>
              </Link>
            ))}
          </div>
        </div>
      )}

      <div>
        <strong>Nearby jobs</strong>
        <p className="subtitle" style={{ fontSize: 13 }}>Map view isn't available yet in this build; showing the list below.</p>
        <div className="selection-list" style={{ marginTop: 8 }}>
          {jobs === null ? (
            <div className="skeleton" style={{ height: 100 }} />
          ) : jobs.length === 0 ? (
            <div className="empty-state">No nearby jobs right now. Make sure your coverage areas are set in Profile.</div>
          ) : jobs.map((j) => (
            <div key={j.id} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <strong>{formatPaise(j.partnerEarningPaise)}</strong>
                <span className="badge badge-neutral">{j.areaName}, {j.cityName}</span>
              </div>
              <p className="subtitle" style={{ margin: '6px 0' }}>{j.notes}</p>
              <p className="subtitle" style={{ fontSize: 12 }}>{new Date(j.scheduledAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' })}</p>
              <Button disabled={busyId === j.id} onClick={() => accept(j.id)}>{busyId === j.id ? 'Accepting…' : 'Accept job'}</Button>
            </div>
          ))}
        </div>
      </div>
      {error && <div className="field error">{error}</div>}
    </div>
  )
}
