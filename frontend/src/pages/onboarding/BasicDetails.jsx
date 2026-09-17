import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import TextField from '../../components/TextField'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

function BasicDetailsForm() {
  const { refresh } = usePartner()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [language, setLanguage] = useState('en')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api.patch('/partners/me/basic-details', { name, preferredLanguage: language })
      await refresh()
      navigate('/onboarding/category')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your details.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen no-bottom-pad">
      <OnboardingHeader title="Basic details" step="basic-details" onBack={() => navigate('/')} />
      <form onSubmit={onSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        <TextField id="name" label="Full name" value={name} onChange={(e) => setName(e.target.value)} required autoFocus />
        <div className="field">
          <label htmlFor="lang">Preferred language</label>
          <select id="lang" value={language} onChange={(e) => setLanguage(e.target.value)}>
            <option value="en">English</option>
            <option value="hi">हिन्दी (Hindi)</option>
          </select>
        </div>
        {error && <div className="field error">{error}</div>}
        <Button type="submit" block disabled={loading}>{loading ? 'Saving…' : 'Continue'}</Button>
      </form>
    </div>
  )
}

export default function BasicDetails() {
  return (
    <OnboardingResumeGuard forStage="BASIC_DETAILS">
      <BasicDetailsForm />
    </OnboardingResumeGuard>
  )
}
