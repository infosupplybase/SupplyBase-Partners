import { useEffect, useState } from 'react'
import { api } from '../../api/client'

export default function AdminAuditLogs() {
  const [page, setPage] = useState(null)

  useEffect(() => {
    api.get('/admin/audit-logs?page=0&size=50').then(setPage).catch(() => setPage(null))
  }, [])

  return (
    <div>
      <h2 className="title">Audit logs</h2>
      {!page ? (
        <div className="skeleton" style={{ height: 200, marginTop: 16 }} />
      ) : (
        <table className="data-table" style={{ marginTop: 16 }}>
          <thead><tr><th>Actor</th><th>Action</th><th>Entity</th><th>Reason</th><th>When</th></tr></thead>
          <tbody>
            {page.content.map((log) => (
              <tr key={log.id}>
                <td>{log.actorUserId ?? 'system'}</td>
                <td>{log.action}</td>
                <td>{log.entityType} #{log.entityId}</td>
                <td>{log.reason || '—'}</td>
                <td>{new Date(log.createdAt).toLocaleString('en-IN')}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
