import { useNavigate } from 'react-router-dom'

const STEPS = [
  'basic-details', 'category', 'city', 'terms', 'earning-potential',
  'working-hours', 'permissions',
]

export default function OnboardingHeader({ title, step, onBack }) {
  const navigate = useNavigate()
  const index = STEPS.indexOf(step)
  return (
    <div className="header" style={{ flexDirection: 'column', height: 'auto', paddingBottom: 10, gap: 8 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%' }}>
        <button className="back-btn" onClick={onBack || (() => navigate(-1))} aria-label="Back">←</button>
        <h1>{title}</h1>
      </div>
      {index >= 0 && (
        <div className="stepper" role="progressbar" aria-valuenow={index + 1} aria-valuemin={1} aria-valuemax={STEPS.length}>
          {STEPS.map((s, i) => (
            <span key={s} className={`dot ${i < index ? 'done' : i === index ? 'current' : ''}`} />
          ))}
        </div>
      )}
    </div>
  )
}
