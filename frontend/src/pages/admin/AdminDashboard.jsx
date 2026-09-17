import { useEffect, useState } from 'react'
import { api } from '../../api/client'

export default function AdminDashboard() {
  const [counts, setCounts] = useState(null)

  useEffect(() => {
    api.get('/admin/dashboard').then(setCounts).catch(() => setCounts(null))
  }, [])

  return (
    <div>
      <h2 className="title">Dashboard</h2>
      {!counts ? (
        <div className="skeleton" style={{ height: 100, marginTop: 16 }} />
      ) : (
        <div className="stat-grid" style={{ marginTop: 16 }}>
          <div className="stat-card"><div className="value">{counts.totalPartners}</div><div className="label">Total partners</div></div>
          <div className="stat-card"><div className="value">{counts.activePartners}</div><div className="label">Active partners</div></div>
          <div className="stat-card"><div className="value">{counts.pendingVerifications}</div><div className="label">Pending verifications</div></div>
          <div className="stat-card"><div className="value">{counts.openJobs}</div><div className="label">Open jobs</div></div>
          <div className="stat-card"><div className="value">{counts.kitOrdersProcessing}</div><div className="label">Kit orders processing</div></div>
        </div>
      )}
    </div>
  )
}
