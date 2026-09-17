import { NavLink, Outlet } from 'react-router-dom'

const TABS = [
  { to: '/app/progress', icon: '📈', label: 'Progress' },
  { to: '/app/money', icon: '💰', label: 'Money' },
  { to: '/app/around-you', icon: '🗺️', label: 'Around You' },
  { to: '/app/community', icon: '💬', label: 'Community' },
]

export default function AppShell() {
  return (
    <div className="app-shell with-sidebar">
      <nav className="sidebar" aria-label="Main">
        {TABS.map((tab) => (
          <NavLink key={tab.to} to={tab.to} className={({ isActive }) => (isActive ? 'active' : '')}>
            <span aria-hidden="true">{tab.icon}</span> {tab.label}
          </NavLink>
        ))}
        <NavLink to="/app/profile" className={({ isActive }) => (isActive ? 'active' : '')}>
          <span aria-hidden="true">👤</span> Profile
        </NavLink>
        <NavLink to="/app/support" className={({ isActive }) => (isActive ? 'active' : '')}>
          <span aria-hidden="true">🆘</span> Support
        </NavLink>
        <NavLink to="/app/notifications" className={({ isActive }) => (isActive ? 'active' : '')}>
          <span aria-hidden="true">🔔</span> Notifications
        </NavLink>
      </nav>
      <div className="main-with-sidebar">
        <Outlet />
        <nav className="bottom-nav" aria-label="Main">
          {TABS.map((tab) => (
            <NavLink key={tab.to} to={tab.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              <span className="nav-icon" aria-hidden="true">{tab.icon}</span>
              {tab.label}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  )
}
