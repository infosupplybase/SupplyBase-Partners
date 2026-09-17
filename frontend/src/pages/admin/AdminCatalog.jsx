import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

export default function AdminCatalog() {
  const [categories, setCategories] = useState(null)
  const [cities, setCities] = useState(null)
  const [categoryName, setCategoryName] = useState('')
  const [cityName, setCityName] = useState('')
  const [cityState, setCityState] = useState('')
  const [error, setError] = useState('')

  function load() {
    api.get('/catalog/categories').then(setCategories).catch(() => setCategories([]))
    api.get('/catalog/cities').then(setCities).catch(() => setCities([]))
  }
  useEffect(load, [])

  function slugify(name) {
    return name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '')
  }

  async function createCategory() {
    if (!categoryName.trim()) return
    setError('')
    try {
      await api.post('/admin/catalog/categories', { name: categoryName, slug: slugify(categoryName) })
      setCategoryName('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create category.')
    }
  }

  async function toggleCategory(cat) {
    await api.patch(`/admin/catalog/categories/${cat.id}`, { active: false }).catch(() => {})
    load()
  }

  async function createCity() {
    if (!cityName.trim() || !cityState.trim()) return
    setError('')
    try {
      await api.post('/admin/catalog/cities', { name: cityName, state: cityState })
      setCityName('')
      setCityState('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create city.')
    }
  }

  return (
    <div>
      <h2 className="title">Catalog</h2>
      {error && <div className="field error">{error}</div>}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginTop: 16 }}>
        <div>
          <strong>Categories</strong>
          <table className="data-table" style={{ marginTop: 8 }}>
            <thead><tr><th>Name</th><th>Slug</th><th></th></tr></thead>
            <tbody>
              {(categories || []).map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td><td>{c.slug}</td>
                  <td><button className="btn btn-ghost" style={{ minHeight: 'auto', padding: 0 }} onClick={() => toggleCategory(c)}>Deactivate</button></td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="card" style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <TextField id="catName" label="New category name" value={categoryName} onChange={(e) => setCategoryName(e.target.value)} />
            <Button onClick={createCategory}>Add category</Button>
          </div>
        </div>

        <div>
          <strong>Cities</strong>
          <table className="data-table" style={{ marginTop: 8 }}>
            <thead><tr><th>Name</th><th>State</th></tr></thead>
            <tbody>
              {(cities || []).map((c) => (
                <tr key={c.id}><td>{c.name}</td><td>{c.state}</td></tr>
              ))}
            </tbody>
          </table>
          <div className="card" style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <TextField id="cityName" label="New city name" value={cityName} onChange={(e) => setCityName(e.target.value)} />
            <TextField id="cityState" label="State" value={cityState} onChange={(e) => setCityState(e.target.value)} />
            <Button onClick={createCity}>Add city</Button>
          </div>
        </div>
      </div>
    </div>
  )
}
