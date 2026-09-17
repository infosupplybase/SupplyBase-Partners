import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Button from '../../components/Button'

export default function AdminVerification() {
  const [page, setPage] = useState(null)

  function load() {
    api.get('/admin/verification/pending?page=0&size=50').then(setPage).catch(() => setPage(null))
  }
  useEffect(load, [])

  async function decide(partnerId, approve) {
    const reason = approve ? '' : window.prompt('Reason for rejection?') || ''
    if (!approve && !reason) return
    await api.post(`/admin/verification/${partnerId}/decision`, { approve, reason }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Pending verifications</h2>
      {!page ? (
        <div className="skeleton" style={{ height: 200, marginTop: 16 }} />
      ) : page.content.length === 0 ? (
        <div className="empty-state">Nothing pending review.</div>
      ) : (
        <table className="data-table" style={{ marginTop: 16 }}>
          <thead><tr><th>Provider</th><th>Submitted</th><th>Actions</th></tr></thead>
          <tbody>
            {page.content.map((v, i) => (
              <tr key={i}>
                <td>{v.provider}</td>
                <td>{v.submittedAt ? new Date(v.submittedAt).toLocaleString('en-IN') : '—'}</td>
                <td style={{ display: 'flex', gap: 8 }}>
                  <Button onClick={() => decide(v.partnerId, true)}>Approve</Button>
                  <Button variant="danger" onClick={() => decide(v.partnerId, false)}>Reject</Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
