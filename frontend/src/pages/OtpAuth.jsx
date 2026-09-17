import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import Button from '../components/Button'
import TextField from '../components/TextField'
import { useAuth } from '../context/AuthContext'

const RESEND_POLL_MS = 1000

export default function OtpAuth() {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [stage, setStage] = useState('phone') // phone | code
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [resendAvailableAt, setResendAvailableAt] = useState(null)
  const [devCode, setDevCode] = useState(null)
  const [, setTick] = useState(0)
  const { refresh } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!resendAvailableAt) return
    const interval = setInterval(() => setTick((t) => t + 1), RESEND_POLL_MS)
    return () => clearInterval(interval)
  }, [resendAvailableAt])

  const secondsUntilResend = resendAvailableAt
    ? Math.max(0, Math.ceil((new Date(resendAvailableAt).getTime() - Date.now()) / 1000))
    : 0

  async function requestOtp(e) {
    e?.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await api.post('/auth/otp/request', { phone })
      setResendAvailableAt(res.resendAvailableAt)
      setStage('code')
      // Best-effort dev convenience: silently ignored (400) when not in dev_local mode.
      try {
        const dev = await api.get(`/auth/otp/dev/last-code?phone=${encodeURIComponent(phone)}`)
        setDevCode(dev.code)
      } catch {
        setDevCode(null)
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  async function verifyOtp(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await api.post('/auth/otp/verify', { phone, code })
      const session = await refresh()
      const target = location.state?.from?.pathname || '/onboarding/basic-details'
      navigate(target === '/login' || target === '/join' ? '/onboarding/basic-details' : target, { replace: true })
      void session
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not verify code.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="screen no-bottom-pad">
      <button className="back-btn" style={{ alignSelf: 'flex-start' }} onClick={() => navigate('/')} aria-label="Back">←</button>
      <h2 className="title">{stage === 'phone' ? 'Enter your mobile number' : 'Enter the code we sent you'}</h2>
      <p className="subtitle">
        {stage === 'phone'
          ? "We'll send a one-time code by SMS to verify it's you."
          : `Enter the 6-digit code sent to ${phone}.`}
      </p>

      {stage === 'phone' ? (
        <form onSubmit={requestOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField
            id="phone" label="Mobile number" type="tel" inputMode="numeric" placeholder="98765 43210"
            value={phone} onChange={(e) => setPhone(e.target.value)} required autoFocus
          />
          {error && <div className="field error">{error}</div>}
          <Button type="submit" block disabled={loading}>{loading ? 'Sending…' : 'Send OTP'}</Button>
        </form>
      ) : (
        <form onSubmit={verifyOtp} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField
            id="code" label="6-digit code" type="text" inputMode="numeric" maxLength={6}
            value={code} onChange={(e) => setCode(e.target.value)} required autoFocus
          />
          {devCode && (
            <div className="badge badge-neutral" style={{ alignSelf: 'flex-start' }}>
              Dev mode &mdash; code: {devCode}
            </div>
          )}
          {error && <div className="field error">{error}</div>}
          <Button type="submit" block disabled={loading}>{loading ? 'Verifying…' : 'Verify & continue'}</Button>
          <Button
            type="button" variant="ghost" block
            disabled={secondsUntilResend > 0}
            onClick={requestOtp}
          >
            {secondsUntilResend > 0 ? `Resend code in ${secondsUntilResend}s` : 'Resend code'}
          </Button>
        </form>
      )}
    </div>
  )
}
