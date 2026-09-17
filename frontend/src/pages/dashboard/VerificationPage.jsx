import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

const STATUS_BADGE = {
  NOT_STARTED: ['badge-neutral', 'Not started'],
  SUBMITTED: ['badge-warning', 'Submitted'],
  PENDING_REVIEW: ['badge-warning', 'Pending review'],
  APPROVED: ['badge-success', 'Approved'],
  REJECTED: ['badge-danger', 'Rejected'],
}

export default function VerificationPage() {
  const navigate = useNavigate()
  const { refresh } = usePartner()
  const showToast = useToast()
  const [status, setStatus] = useState(null)
  const [docType, setDocType] = useState('AADHAAR')
  const [file, setFile] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  function load() {
    api.get('/partners/me/verification').then(setStatus).catch(() => setStatus(null))
  }
  useEffect(load, [])

  async function onSubmit(e) {
    e.preventDefault()
    if (!file) return
    setSubmitting(true)
    setError('')
    try {
      const form = new FormData()
      form.append('docType', docType)
      form.append('file', file)
      await api.postForm(`/partners/me/verification?docType=${docType}`, form)
      showToast('Document submitted for review.')
      load()
      await refresh()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not submit your document.')
    } finally {
      setSubmitting(false)
    }
  }

  const [badgeClass, badgeLabel] = status ? STATUS_BADGE[status.status] || ['badge-neutral', status.status] : ['badge-neutral', '…']

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Identity verification</h1>
      </div>
      <span className={`badge ${badgeClass}`} style={{ alignSelf: 'flex-start' }}>{badgeLabel}</span>

      {status?.status === 'REJECTED' && status.reason && (
        <div className="card" style={{ background: 'var(--color-danger-soft)', border: 'none' }}>
          <strong>Resubmission needed</strong>
          <p className="subtitle" style={{ marginTop: 4 }}>{status.reason}</p>
        </div>
      )}

      {status?.status === 'APPROVED' ? (
        <div className="card empty-state">Your identity has been verified.</div>
      ) : (
        <form onSubmit={onSubmit} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
          <div className="field">
            <label htmlFor="docType">Document type</label>
            <select id="docType" value={docType} onChange={(e) => setDocType(e.target.value)}>
              <option value="AADHAAR">Aadhaar</option>
              <option value="PAN">PAN</option>
              <option value="OTHER">Other government ID</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="file">Upload document (JPEG, PNG or PDF, max 10MB)</label>
            <input id="file" type="file" accept="image/jpeg,image/png,image/webp,application/pdf" onChange={(e) => setFile(e.target.files?.[0] || null)} required />
          </div>
          {error && <div className="field error">{error}</div>}
          <Button type="submit" disabled={!file || submitting}>{submitting ? 'Submitting…' : 'Submit for review'}</Button>
        </form>
      )}
      <p className="subtitle">
        A manual review by our team approves this document; it is not an automated government verification in this demo build.
      </p>
    </div>
  )
}
