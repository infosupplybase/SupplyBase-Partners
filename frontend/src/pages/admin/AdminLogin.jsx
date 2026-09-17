import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { useAuth } from '../../context/AuthContext'

export default function AdminLogin() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { refresh } = useAuth()
  const navigate = useNavigate()

  async function onSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api.post('/auth/staff/login', { username, password })
      await refresh()
      navigate('/admin/dashboard', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Invalid username or password.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--color-accent-dark)' }}>
      <form onSubmit={onSubmit} className="card" style={{ width: 340, display: 'flex', flexDirection: 'column', gap: 16 }}>
        <h2 className="title">SupplyBase Admin</h2>
        <TextField id="username" label="Username" value={username} onChange={(e) => setUsername(e.target.value)} required autoFocus />
        <TextField id="password" label="Password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        {error && <div className="field error">{error}</div>}
        <Button type="submit" block disabled={loading}>{loading ? 'Signing in…' : 'Sign in'}</Button>
      </form>
    </div>
  )
}
