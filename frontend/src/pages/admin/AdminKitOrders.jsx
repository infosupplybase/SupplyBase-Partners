import { useEffect, useState } from 'react'
import { api } from '../../api/client'
import Button from '../../components/Button'

function formatPaise(paise) {
  return `₹${(paise / 100).toLocaleString('en-IN')}`
}

const NEXT_STATUS = {
  PENDING_PAYMENT: null, PAID: 'PROCESSING', PROCESSING: 'SHIPPED', SHIPPED: 'DELIVERED',
  DELIVERED: null, CANCELLED: null, REFUNDED: null, PAYMENT_FAILED: null, QUOTE: null,
}

export default function AdminKitOrders() {
  const [page, setPage] = useState(null)

  function load() {
    api.get('/admin/kit-orders?page=0&size=50').then(setPage).catch(() => setPage(null))
  }
  useEffect(load, [])

  async function advance(orderId, status) {
    await api.patch(`/admin/kit-orders/${orderId}/status`, { status }).catch(() => {})
    load()
  }

  return (
    <div>
      <h2 className="title">Starter kit orders</h2>
      {!page ? (
        <div className="skeleton" style={{ height: 200, marginTop: 16 }} />
      ) : page.content.length === 0 ? (
        <div className="empty-state">No orders yet.</div>
      ) : (
        <table className="data-table" style={{ marginTop: 16 }}>
          <thead><tr><th>Order</th><th>Delivery address</th><th>Total</th><th>Status</th><th>Action</th></tr></thead>
          <tbody>
            {page.content.map((o) => {
              const next = NEXT_STATUS[o.status]
              return (
                <tr key={o.id}>
                  <td>#{o.id}</td>
                  <td>{o.deliveryAddressLine}</td>
                  <td>{formatPaise(o.totalPaise)}</td>
                  <td>{o.status.replace('_', ' ')}</td>
                  <td>{next && <Button variant="secondary" onClick={() => advance(o.id, next)}>Mark {next.toLowerCase()}</Button>}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      )}
    </div>
  )
}
