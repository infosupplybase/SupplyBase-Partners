import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import OnboardingHeader from '../../components/OnboardingHeader'
import SelectionCard from '../../components/SelectionCard'
import { OnboardingResumeGuard } from '../../components/RouteGuards'
import { usePartner } from '../../context/PartnerContext'

const ICONS = {
  electrical: '⚡', plumbing: '🔧', painting: '🎨', 'pop-ceiling-design': '🏛️',
  waterproofing: '💧', 'interior-design': '🛋️', cleaning: '🧹',
}

function WorkCategoryForm() {
  const { partner, refresh } = usePartner()
  const navigate = useNavigate()
  const [categories, setCategories] = useState(null)
  const [query, setQuery] = useState('')
  const [selectedId, setSelectedId] = useState(partner?.primaryCategoryId ?? null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    api.get('/catalog/categories').then(setCategories).catch(() => setCategories([]))
  }, [])

  const filtered = useMemo(() => {
    if (!categories) return []
    const q = query.trim().toLowerCase()
    return q ? categories.filter((c) => c.name.toLowerCase().includes(q)) : categories
  }, [categories, query])

  async function onContinue() {
    if (!selectedId) return
    setLoading(true)
    setError('')
    try {
      await api.put('/partners/me/category', { categoryId: selectedId })
      await refresh()
      navigate('/onboarding/city')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your category.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen">
      <OnboardingHeader title="What work do you do?" step="category" />
      <p className="subtitle">Choose the primary service you'll offer. You can add more skills later.</p>
      <input
        aria-label="Search categories" placeholder="Search categories"
        value={query} onChange={(e) => setQuery(e.target.value)}
        style={{ border: '1px solid var(--color-border)', borderRadius: 10, padding: 12, minHeight: 44 }}
      />
      {categories === null ? (
        <div className="selection-list">
          {[1, 2, 3, 4].map((i) => <div key={i} className="skeleton" style={{ height: 68, borderRadius: 14 }} />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state">No categories match your search.</div>
      ) : (
        <div className="selection-list">
          {filtered.map((c) => (
            <SelectionCard
              key={c.id}
              selected={selectedId === c.id}
              icon={c.iconUrl ? undefined : ICONS[c.slug] || '🧰'}
              label={c.name}
              onClick={() => setSelectedId(c.id)}
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

export default function WorkCategory() {
  return (
    <OnboardingResumeGuard forStage="WORK_CATEGORY">
      <WorkCategoryForm />
    </OnboardingResumeGuard>
  )
}
