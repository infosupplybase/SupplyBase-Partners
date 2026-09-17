import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from './AuthContext'

const PartnerContext = createContext(null)

// Maps backend onboardingStage -> the route that stage should land on. The
// first seven stages are a linear full-screen wizard (A-H in the build
// brief); from VERIFICATION onward, progress is tracked as tiles on the
// dashboard's Progress tab (section K) rather than a forced wizard, since a
// partner can be mid-review on any of Verification/Screening/Kit/Training in
// any order of attention, not strictly sequential from the user's viewpoint.
export const STAGE_TO_ROUTE = {
  BASIC_DETAILS: '/onboarding/basic-details',
  WORK_CATEGORY: '/onboarding/category',
  CITY: '/onboarding/city',
  TERMS_PRIVACY: '/onboarding/terms',
  EARNING_POTENTIAL: '/onboarding/earning-potential',
  WORKING_HOURS: '/onboarding/working-hours',
  PERMISSIONS: '/onboarding/permissions',
  VERIFICATION: '/app/progress',
  SCREENING: '/app/progress',
  STARTER_KIT: '/app/progress',
  PROFILE: '/app/progress',
  TRAINING: '/app/progress',
  ACTIVATION_REVIEW: '/app/progress',
  COMPLETE: '/app/progress',
}

export function PartnerProvider({ children }) {
  const { session } = useAuth()
  const [partner, setPartner] = useState(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!session?.authenticated) {
      setPartner(null)
      return null
    }
    const data = await api.get('/partners/me')
    setPartner(data)
    return data
  }, [session])

  useEffect(() => {
    if (session === null) return
    refresh().finally(() => setLoading(false))
  }, [session, refresh])

  return (
    <PartnerContext.Provider value={{ partner, loading, refresh }}>
      {children}
    </PartnerContext.Provider>
  )
}

export function usePartner() {
  const ctx = useContext(PartnerContext)
  if (!ctx) throw new Error('usePartner must be used within PartnerProvider')
  return ctx
}
