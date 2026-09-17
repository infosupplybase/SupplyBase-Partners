import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const STAFF_ROLES = ['ADMIN', 'REVIEWER', 'TRAINER', 'FINANCE']

export default function RequireStaff() {
  const { session, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  const isStaff = session?.authenticated && session.roles?.some((r) => STAFF_ROLES.includes(r))
  if (!isStaff) {
    return <Navigate to="/admin/login" state={{ from: location }} replace />
  }
  return <Outlet />
}
