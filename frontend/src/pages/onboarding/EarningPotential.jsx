import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

function formatPaise(paise) {
  return `₹${Math.round(paise / 100).toLocaleString('en-IN')}`
}

function EarningPotentialForm() {
  const { partner, refresh } = usePartner()
  const navigate = useNavigate()
  const [estimate, setEstimate] = useState(undefined)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!partner?.primaryCategoryId || !partner?.residenceCityId) return
    api.get(`/catalog/estimates?categoryId=${partner.primaryCategoryId}&cityId=${partner.residenceCityId}&hours=EIGHT`)
      .then(setEstimate)
      .catch(() => setEstimate({ available: false }))
  }, [partner])

  async function onContinue() {
    setLoading(true)
    setError('')
    try {
      await api.post('/partners/me/earning-potential/ack')
      await refresh()
      navigate('/onboarding/working-hours')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not continue.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="Your earning potential" step="earning-potential" />
      {estimate === undefined ? (
        <div className="skeleton" style={{ height: 140 }} />
      ) : estimate.available ? (
        <div className="card" style={{ textAlign: 'center', background: 'var(--color-accent-soft)', border: 'none' }}>
          <div className="badge badge-neutral" style={{ marginBottom: 8 }}>Demo estimate</div>
          <div style={{ fontSize: 30, fontWeight: 800, color: 'var(--color-accent-dark)' }}>
            Up to {formatPaise(estimate.estimatedMonthlyPaise)} / month
          </div>
          <p className="subtitle" style={{ marginTop: 10 }}>{estimate.assumptionsText}</p>
        </div>
      ) : (
        <div className="card empty-state">
          An earning estimate isn't available yet for your category and city. Actual earnings depend on jobs completed.
        </div>
      )}
      <p className="subtitle">
        This is an illustrative demo estimate, not a confirmed earning or a guarantee. Real earnings depend on
        hours worked, local demand, and jobs completed.
      </p>
      {error && <div className="field error">{error}</div>}
      <Button block disabled={loading} onClick={onContinue}>{loading ? 'Saving…' : 'Continue'}</Button>
    </div>
  )
}

export default function EarningPotential() {
  return (
    <OnboardingResumeGuard forStage="EARNING_POTENTIAL">
      <EarningPotentialForm />
    </OnboardingResumeGuard>
  )
}
