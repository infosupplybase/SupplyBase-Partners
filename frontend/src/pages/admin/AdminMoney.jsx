import { useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

export default function AdminMoney() {
  const [partnerId, setPartnerId] = useState('')
  const [type, setType] = useState('BONUS')
  const [amount, setAmount] = useState('')
  const [reason, setReason] = useState('')
  const [earningEntryId, setEarningEntryId] = useState('')
  const [reverseReason, setReverseReason] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  async function adjust() {
    setError(''); setMessage('')
    try {
      await api.post(`/admin/money/partners/${partnerId}/adjustments`, {
        type, amountPaise: Math.round(Number(amount) * 100), reason,
      })
      setMessage('Adjustment recorded.')
      setAmount(''); setReason('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not record adjustment.')
    }
  }

  async function reverse() {
    setError(''); setMessage('')
    try {
      await api.post(`/admin/money/earnings/${earningEntryId}/reverse`, { reason: reverseReason })
      setMessage('Earning entry reversed.')
      setEarningEntryId(''); setReverseReason('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not reverse this entry.')
    }
  }

  return (
    <div>
      <h2 className="title">Money</h2>
      {error && <div className="field error">{error}</div>}
      {message && <div className="badge badge-success" style={{ marginTop: 8 }}>{message}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginTop: 16 }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>Earning adjustment</strong>
          <TextField id="adjPartnerId" label="Partner ID" value={partnerId} onChange={(e) => setPartnerId(e.target.value)} />
          <div className="field">
            <label>Type</label>
            <select value={type} onChange={(e) => setType(e.target.value)}>
              <option value="BONUS">Bonus</option>
              <option value="PENALTY">Penalty</option>
              <option value="DEDUCTION">Deduction</option>
            </select>
          </div>
          <TextField id="adjAmount" label="Amount (₹)" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
          <TextField id="adjReason" label="Reason" value={reason} onChange={(e) => setReason(e.target.value)} />
          <Button disabled={!partnerId || !amount || !reason} onClick={adjust}>Record adjustment</Button>
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>Reverse an earning entry</strong>
          <TextField id="earningEntryId" label="Earning entry ID" value={earningEntryId} onChange={(e) => setEarningEntryId(e.target.value)} />
          <TextField id="reverseReason" label="Reason" value={reverseReason} onChange={(e) => setReverseReason(e.target.value)} />
          <Button variant="danger" disabled={!earningEntryId || !reverseReason} onClick={reverse}>Reverse entry</Button>
        </div>
      </div>
    </div>
  )
}
