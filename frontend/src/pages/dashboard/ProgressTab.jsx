import { Link } from 'react-router-dom'
import { usePartner } from '../../context/PartnerContext'

const STAGE_ORDER = [
  'BASIC_DETAILS', 'WORK_CATEGORY', 'CITY', 'TERMS_PRIVACY', 'EARNING_POTENTIAL',
  'WORKING_HOURS', 'PERMISSIONS', 'VERIFICATION', 'SCREENING', 'STARTER_KIT',
  'PROFILE', 'TRAINING', 'ACTIVATION_REVIEW', 'COMPLETE',
]

const TILES = [
  { stage: 'VERIFICATION', title: 'Identity verification', icon: '🪪', to: '/app/progress/verification' },
  { stage: 'SCREENING', title: 'Skill session', icon: '🎯', to: '/app/progress/screening' },
  { stage: 'STARTER_KIT', title: 'Starter kit', icon: '📦', to: '/app/progress/starter-kit' },
  { stage: 'PROFILE', title: 'Profile', icon: '👤', to: '/app/progress/profile' },
  { stage: 'TRAINING', title: 'Training', icon: '🎓', to: '/app/progress/training' },
]

function tileStatus(currentIndex, tileIndex) {
  if (currentIndex < tileIndex) return 'locked'
  if (currentIndex === tileIndex) return 'current'
  return 'complete'
}

function StatusBadge({ status }) {
  if (status === 'locked') return <span className="badge badge-neutral">Locked</span>
  if (status === 'current') return <span className="badge badge-warning">In progress</span>
  return <span className="badge badge-success">Complete</span>
}

export default function ProgressTab() {
  const { partner, loading } = usePartner()
  if (loading || !partner) {
    return <div className="screen"><div className="skeleton" style={{ height: 200 }} /></div>
  }
  const currentIndex = STAGE_ORDER.indexOf(partner.onboardingStage)

  return (
    <div className="screen">
      <h2 className="title">Your progress</h2>
      {partner.activationStatus === 'ACTIVE' ? (
        <div className="card" style={{ background: 'var(--color-accent-soft)', border: 'none' }}>
          <strong>You're an active SupplyBase partner 🎉</strong>
          <p className="subtitle" style={{ marginTop: 6 }}>Head to Around You to find nearby jobs.</p>
        </div>
      ) : partner.activationStatus === 'SUSPENDED' ? (
        <div className="card" style={{ background: 'var(--color-danger-soft)', border: 'none' }}>
          <strong>Your account is suspended</strong>
          <p className="subtitle" style={{ marginTop: 6 }}>{partner.suspendedReason || 'Contact support for details.'}</p>
        </div>
      ) : (
        <p className="subtitle">Complete each step below to activate your account and start taking jobs.</p>
      )}

      <div className="selection-list">
        {TILES.map((tile) => {
          const tileIndex = STAGE_ORDER.indexOf(tile.stage)
          const status = tileStatus(currentIndex, tileIndex)
          const content = (
            <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
              <span style={{ fontSize: 24 }} aria-hidden="true">{tile.icon}</span>
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: 700 }}>{tile.title}</div>
              </div>
              <StatusBadge status={status} />
            </div>
          )
          return status === 'locked' ? (
            <div key={tile.stage} style={{ opacity: 0.6 }}>{content}</div>
          ) : (
            <Link key={tile.stage} to={tile.to} style={{ textDecoration: 'none', color: 'inherit' }}>{content}</Link>
          )
        })}
      </div>
    </div>
  )
}
