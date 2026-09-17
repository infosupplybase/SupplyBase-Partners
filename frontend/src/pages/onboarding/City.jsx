import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import SelectionCard from '../../components/SelectionCard'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

function CityForm() {
  const { refresh } = usePartner()
  const navigate = useNavigate()
  const [cities, setCities] = useState(null)
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/catalog/cities').then(setCities).catch(() => setCities([]))
  }, [])

  const filtered = useMemo(() => {
    if (!cities) return []
    const q = query.trim().toLowerCase()
    return q ? cities.filter((c) => c.name.toLowerCase().includes(q)) : cities
  }, [cities, query])

  async function onContinue() {
    if (!selectedId) return
    setLoading(true)
    setError('')
    try {
      await api.put('/partners/me/city', { cityId: selectedId })
      await refresh()
      navigate('/onboarding/terms')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your city.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="Where do you live?" step="city" />
      <p className="subtitle">Manual selection works even without location access. You can add coverage areas later.</p>
      <input
        aria-label="Search cities" placeholder="Search cities"
        value={query} onChange={(e) => setQuery(e.target.value)}
        style={{ border: '1px solid var(--color-border)', borderRadius: 10, padding: 12, minHeight: 44 }}
      />
      {cities === null ? (
        <div className="selection-list">
          {[1, 2, 3].map((i) => <div key={i} className="skeleton" style={{ height: 68, borderRadius: 14 }} />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">No cities match your search.</div>
      ) : (
        <div className="selection-list">
          {filtered.map((c) => (
            <SelectionCard
              key={c.id} selected={selectedId === c.id} icon="📍"
              label={c.name} meta={c.state} onClick={() => setSelectedId(c.id)}
            />
          ))}
        </div>
      )}
      {error && <div className="field error">{error}</div>}
      <Button block disabled={!selectedId || loading} onClick={onContinue}>
        {loading ? 'Saving…' : 'Continue'}
      </Button>
    </div>
  )
}

export default function City() {
  return (
    <OnboardingResumeGuard forStage="CITY">
      <CityForm />
    </OnboardingResumeGuard>
  )
}
