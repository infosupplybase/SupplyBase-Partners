import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

function PolicyCard({ doc }) {
  const [expanded, setExpanded] = useState(false)
  if (!doc) return null
  return (
    <div className="card">
      <button
        type="button" onClick={() => setExpanded((v) => !v)}
        style={{ background: 'none', border: 'none', padding: 0, cursor: 'pointer', textAlign: 'left', width: '100%' }}
      >
        <strong>{doc.title}</strong> <span style={{ color: 'var(--color-accent)' }}>{expanded ? 'Hide' : 'Read'}</span>
      </button>
      {expanded && (
        <div style={{ marginTop: 10, fontSize: 14, color: 'var(--color-text-muted)', whiteSpace: 'pre-wrap', maxHeight: 220, overflowY: 'auto' }}>
          {doc.contentMarkdown}
        </div>
      )}
    </div>
  )
}

function TermsPrivacyForm() {
  const { refresh } = usePartner()
  const navigate = useNavigate()
  const [policies, setPolicies] = useState(null)
  const [acceptedRequired, setAcceptedRequired] = useState(false)
  const [marketingOptIn, setMarketingOptIn] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/catalog/policies/current').then(setPolicies).catch(() => setPolicies([]))
  }, [])

  const terms = policies?.find((p) => p.type === 'TERMS')
  const privacy = policies?.find((p) => p.type === 'PRIVACY')
  const marketing = policies?.find((p) => p.type === 'MARKETING_CONSENT')

  async function onContinue() {
    setLoading(true)
    setError('')
    try {
      await api.post('/partners/me/consents', { marketingOptIn })
      await refresh()
      navigate('/onboarding/earning-potential')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your consent.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="Terms & privacy" step="terms" />
      <p className="subtitle">
        These are draft documents pending business/legal review, shown here so the onboarding flow works end to end.
      </p>
      {policies === null ? (
        <div className="skeleton" style={{ height: 120 }} />
      ) : (
        <>
          <PolicyCard doc={terms} />
          <PolicyCard doc={privacy} />
        </>
      )}
      <label style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
        <input type="checkbox" checked={acceptedRequired} onChange={(e) => setAcceptedRequired(e.target.checked)} style={{ marginTop: 3 }} />
        <span>I have read and accept the Terms of Service and Privacy Policy.</span>
      </label>
      {marketing && (
        <label style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
          <input type="checkbox" checked={marketingOptIn} onChange={(e) => setMarketingOptIn(e.target.checked)} style={{ marginTop: 3 }} />
          <span>Send me tips and offers by SMS/WhatsApp (optional).</span>
        </label>
      )}
      {error && <div className="field error">{error}</div>}
      <Button block disabled={!acceptedRequired || loading} onClick={onContinue}>
        {loading ? 'Saving…' : 'Continue'}
      </Button>
    </div>
  )
}

export default function TermsPrivacy() {
  return (
    <OnboardingResumeGuard forStage="TERMS_PRIVACY">
      <TermsPrivacyForm />
    </OnboardingResumeGuard>
  )
}
