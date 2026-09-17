import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/client'

export default function AdminPartners() {
  const [page, setPage] = useState(null)
  const [pageNumber, setPageNumber] = useState(0)

  useEffect(() => {
    api.get(`/admin/partners?page=${pageNumber}&size=20`).then(setPage).catch(() => setPage(null))
  }, [pageNumber])

  return (
    <div>
      <h2 className="title">Partners</h2>
      {!page ? (
        <div className="skeleton" style={{ height: 200, marginTop: 16 }} />
      ) : (
        <>
          <table className="data-table" style={{ marginTop: 16 }}>
            <thead>
              <tr><th>ID</th><th>Category</th><th>City</th><th>Stage</th><th>Status</th></tr>
            </thead>
            <tbody>
              {page.content.map((p) => (
                <tr key={p.id}>
                  <td><Link to={`/admin/partners/${p.id}`}>{p.id}</Link></td>
                  <td>{p.primaryCategoryId ?? '—'}</td>
                  <td>{p.residenceCityId ?? '—'}</td>
                  <td>{p.onboardingStage}</td>
                  <td>{p.activationStatus}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ display: 'flex', gap: 10, marginTop: 12 }}>
            <button className="btn btn-secondary" disabled={pageNumber === 0} onClick={() => setPageNumber((p) => p - 1)}>Previous</button>
            <button className="btn btn-secondary" disabled={page.last} onClick={() => setPageNumber((p) => p + 1)}>Next</button>
          </div>
        </>
      )}
    </div>
  )
}
