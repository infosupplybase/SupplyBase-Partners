import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

function PermissionRow({ icon, title, description, status, onRequest }) {
  return (
    <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <span style={{ fontSize: 26 }} aria-hidden="true">{icon}</span>
      <div style={{ flex: 1 }}>
        <div style={{ fontWeight: 700 }}>{title}</div>
        <div className="meta" style={{ color: 'var(--color-text-muted)', fontSize: 13 }}>{description}</div>
        {status === 'granted' && <span className="badge badge-success" style={{ marginTop: 6 }}>Allowed</span>}
        {status === 'denied' && <span className="badge badge-danger" style={{ marginTop: 6 }}>Denied &mdash; you can enable it later in your browser settings</span>}
        {status === 'unavailable' && <span className="badge badge-warning" style={{ marginTop: 6 }}>Not available on this device/browser</span>}
      </div>
      {status !== 'granted' && (
        <Button variant="secondary" onClick={onRequest}>Allow</Button>
      )}
    </div>
  )
}

function PermissionsForm() {
  const { refresh } = usePartner()
  const navigate = useNavigate()
  const [locationStatus, setLocationStatus] = useState('idle')
  const [cameraStatus, setCameraStatus] = useState('idle')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  function requestLocation() {
    if (!('geolocation' in navigator)) {
      setLocationStatus('unavailable')
      return
    }
    navigator.geolocation.getCurrentPosition(
      () => setLocationStatus('granted'),
      () => setLocationStatus('denied'),
      { timeout: 8000 },
    )
  }

  async function requestCamera() {
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraStatus('unavailable')
      return
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true })
      stream.getTracks().forEach((t) => t.stop())
      setCameraStatus('granted')
    } catch {
      setCameraStatus('denied')
    }
  }

  async function onContinue() {
    setLoading(true)
    setError('')
    try {
      await api.post('/partners/me/permissions/ack')
      await refresh()
      navigate('/app/progress')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not continue.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="Permissions" step="permissions" />
      <p className="subtitle">
        We use these to match you with nearby jobs and let customers see photo proof of completed work.
        You can always enter your city and upload files manually instead.
      </p>
      <PermissionRow
        icon="📍" title="Location" status={locationStatus}
        description="Used to show nearby jobs. You can also select your city and areas manually."
        onRequest={requestLocation}
      />
      <PermissionRow
        icon="📷" title="Camera" status={cameraStatus}
        description="Used later for completion photos. You can also upload existing photos instead."
        onRequest={requestCamera}
      />
      {error && <div className="field error">{error}</div>}
      <Button block disabled={loading} onClick={onContinue}>{loading ? 'Saving…' : 'Continue'}</Button>
    </div>
  )
}

export default function Permissions() {
  return (
    <OnboardingResumeGuard forStage="PERMISSIONS">
      <PermissionsForm />
    </OnboardingResumeGuard>
  )
}
