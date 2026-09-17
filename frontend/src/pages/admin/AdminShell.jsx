import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

const LINKS = [
  { to: '/admin/dashboard', label: 'Dashboard' },
  { to: '/admin/partners', label: 'Partners' },
  { to: '/admin/verification', label: 'Verification' },
  { to: '/admin/catalog', label: 'Catalog' },
  { to: '/admin/screening', label: 'Screening' },
  { to: '/admin/kit-orders', label: 'Kit orders' },
  { to: '/admin/jobs', label: 'Jobs' },
  { to: '/admin/support', label: 'Support' },
  { to: '/admin/community', label: 'Community' },
  { to: '/admin/audit-logs', label: 'Audit logs' },
]

export default function AdminShell() {
  const { logout, session } = useAuth()
  const navigate = useNavigate()

  async function onLogout() {
    await logout()
    navigate('/admin/login', { replace: true })
  }

  return (
    <div className="admin-shell">
      <nav className="admin-sidebar">
        <div style={{ fontWeight: 800, fontSize: 18, padding: '8px 14px 20px', color: 'white' }}>SupplyBase Admin</div>
        {LINKS.map((l) => (
          <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'active' : '')}>{l.label}</NavLink>
        ))}
        <div style={{ flex: 1 }} />
        <div style={{ padding: '10px 14px', color: 'rgba(255,255,255,0.6)', fontSize: 13 }}>{session?.displayName}</div>
        <button onClick={onLogout}>Log out</button>
      </nav>
      <div className="admin-main">
        <Outlet />
      </div>
    </div>
  )
}
