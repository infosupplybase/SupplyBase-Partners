import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

const OPTIONS = [
  { value: 'FOUR', label: '4 hours/day', meta: 'Part-time, flexible' },
  { value: 'SIX', label: '6 hours/day', meta: 'Balanced schedule' },
  { value: 'EIGHT', label: '8 hours/day', meta: 'Full-time, highest earning potential' },
]

function WorkingHoursForm() {
  const { refresh } = usePartner()
  const navigate = useNavigate()
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onContinue() {
    if (!selected) return
    setLoading(true)
    setError('')
    try {
      await api.put('/partners/me/working-hours', { choice: selected })
      await refresh()
      navigate('/onboarding/permissions')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your choice.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="How many hours can you work?" step="working-hours" />
      <p className="subtitle">You can change your availability later in settings.</p>
      <div className="selection-list">
        {OPTIONS.map((opt) => (
          <SelectionCardLarge key={opt.value} {...opt} selected={selected === opt.value} onClick={() => setSelected(opt.value)} />
        ))}
      </div>
      {error && <div className="field error">{error}</div>}
      <Button block disabled={!selected || loading} onClick={onContinue}>{loading ? 'Saving…' : 'Continue'}</Button>
    </div>
  )
}

function SelectionCardLarge({ label, meta, selected, onClick }) {
  return (
    <button
      type="button" onClick={onClick} aria-pressed={selected}
      className={`selection-card${selected ? ' selected' : ''}`}
      style={{ flexDirection: 'column', alignItems: 'flex-start', padding: 20 }}
    >
      <div style={{ fontSize: 20, fontWeight: 700 }}>{label}</div>
      <div className="meta">{meta}</div>
    </button>
  )
}

export default function WorkingHours() {
  return (
    <OnboardingResumeGuard forStage="WORKING_HOURS">
      <WorkingHoursForm />
    </OnboardingResumeGuard>
  )
}
