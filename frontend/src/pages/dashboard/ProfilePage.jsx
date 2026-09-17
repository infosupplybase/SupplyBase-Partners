import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

export default function ProfilePage() {
  const navigate = useNavigate()
  const { partner, refresh } = usePartner()
  const showToast = useToast()
  const [experienceYears, setExperienceYears] = useState(partner?.experienceYears ?? '')
  const [bio, setBio] = useState(partner?.bio ?? '')
  const [languages, setLanguages] = useState(partner?.languages ? Array.from(partner.languages) : [])
  const [bankAccount, setBankAccount] = useState(null)
  const [accountHolderName, setAccountHolderName] = useState('')
  const [accountNumber, setAccountNumber] = useState('')
  const [ifsc, setIfsc] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api.get('/partners/me/bank-account').then(setBankAccount).catch(() => setBankAccount(null))
  }, [])

  function toggleLanguage(code) {
    setLanguages((prev) => (prev.includes(code) ? prev.filter((c) => c !== code) : [...prev, code]))
  }

  async function saveProfile() {
    setBusy(true)
    setError('')
    try {
      await api.put('/partners/me/profile', {
        experienceYears: experienceYears === '' ? null : Number(experienceYears),
        bio,
        languages,
      })
      await refresh()
      showToast('Profile saved.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your profile.')
    } finally {
      setBusy(false)
    }
  }

  async function saveBank() {
    setBusy(true)
    setError('')
    try {
      const result = await api.put('/partners/me/bank-account', { accountHolderName, accountNumber, ifsc })
      setBankAccount(result)
      showToast('Bank details saved.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not save your bank details.')
    } finally {
      setBusy(false)
    }
  }

  async function markComplete() {
    setBusy(true)
    setError('')
    try {
      await saveProfile()
      await api.post('/partners/me/profile/complete')
      await refresh()
      showToast('Profile marked complete.')
      navigate('/app/progress')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Complete all required fields first.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Profile</h1>
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <TextField id="exp" label="Years of experience" type="number" min="0" max="60"
          value={experienceYears} onChange={(e) => setExperienceYears(e.target.value)} />
        <TextField id="bio" label="About you" as="textarea" rows={3}
          value={bio} onChange={(e) => setBio(e.target.value)} />
        <div className="field">
          <label>Languages you speak</label>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            {[['en', 'English'], ['hi', 'Hindi'], ['mr', 'Marathi']].map(([code, label]) => (
              <button key={code} type="button" onClick={() => toggleLanguage(code)}
                className={`badge ${languages.includes(code) ? 'badge-success' : 'badge-neutral'}`}
                style={{ border: 'none', cursor: 'pointer' }}>
                {label}
              </button>
            ))}
          </div>
        </div>
        <Button disabled={busy} onClick={saveProfile}>Save profile</Button>
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        <strong>Payout bank account</strong>
        {bankAccount ? (
          <p className="subtitle">
            {bankAccount.accountHolderName} &middot; {bankAccount.accountNumberMasked} &middot;{' '}
            <span className={`badge ${bankAccount.verificationStatus === 'PROVIDER_VERIFIED' ? 'badge-success' : 'badge-warning'}`}>
              {bankAccount.verificationStatus.replace('_', ' ')}
            </span>
          </p>
        ) : (
          <>
            <TextField id="holder" label="Account holder name" value={accountHolderName} onChange={(e) => setAccountHolderName(e.target.value)} />
            <TextField id="accnum" label="Account number" value={accountNumber} onChange={(e) => setAccountNumber(e.target.value)} />
            <TextField id="ifsc" label="IFSC code" value={ifsc} onChange={(e) => setIfsc(e.target.value.toUpperCase())} />
            <Button variant="secondary" disabled={busy} onClick={saveBank}>Save bank details</Button>
          </>
        )}
      </div>

      {error && <div className="field error">{error}</div>}
      <Button block disabled={busy} onClick={markComplete}>Mark profile complete &amp; continue</Button>
    </div>
  )
}
