import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { PartnerProvider, STAGE_TO_ROUTE, usePartner } from '../context/PartnerContext'

export function RequireAuth() {
  const { session, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  if (!session?.authenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }
  return (
    <PartnerProvider>
      <Outlet />
    </PartnerProvider>
  )
}

/**
 * Client-side convenience only -- the real gate is server-side (every mutating
 * endpoint independently checks preconditions). This just lands a returning
 * user on the step they actually left off at instead of the welcome screen.
 */
export function OnboardingResumeGuard({ children, forStage }) {
  const { partner, loading } = usePartner()
  const location = useLocation()
  if (loading || !partner) return null
  if (partner.onboardingStage !== forStage) {
    const target = STAGE_TO_ROUTE[partner.onboardingStage] || '/app/progress'
    if (target !== location.pathname) {
      return <Navigate to={target} replace />
    }
  }
  return children
}

export function RequireActive({ children }) {
  const { partner, loading } = usePartner()
  if (loading || !partner) return null
  if (partner.activationStatus !== 'ACTIVE') {
    return <Navigate to="/app/progress" replace />
  }
  return children
}
