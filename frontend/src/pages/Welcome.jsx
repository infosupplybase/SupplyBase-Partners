import { useNavigate } from 'react-router-dom'
import Button from '../components/Button'

export default function Welcome() {
  const navigate = useNavigate()
  return (
    <div className="screen no-bottom-pad" style={{ justifyContent: 'space-between', minHeight: '100vh' }}>
      <div />
      <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'center' }}>
        <div
          aria-hidden="true"
          style={{
            width: 96, height: 96, borderRadius: 24, background: 'var(--color-accent-soft)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 44,
          }}
        >
          🛠️
        </div>
        <h2 className="title">SupplyBase Partners</h2>
        <p className="subtitle">
          Join thousands of skilled professionals delivering electrical, plumbing, painting, cleaning
          and more &mdash; on your own schedule, backed by verified jobs and steady earnings.
        </p>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Button block onClick={() => navigate('/join')}>Join as a Partner</Button>
        <Button block variant="ghost" onClick={() => navigate('/login')}>Already registered? Log in</Button>
      </div>
    </div>
  )
}
