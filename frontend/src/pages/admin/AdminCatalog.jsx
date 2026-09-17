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

  const [serviceCategoryId, setServiceCategoryId] = useState('')
  const [serviceName, setServiceName] = useState('')
  const [services, setServices] = useState([])

  const [areaCityId, setAreaCityId] = useState('')
  const [areaName, setAreaName] = useState('')
  const [areas, setAreas] = useState([])

  const [policyType, setPolicyType] = useState('TERMS')
  const [policyVersion, setPolicyVersion] = useState('')
  const [policyTitle, setPolicyTitle] = useState('')
  const [policyContent, setPolicyContent] = useState('')

  function load() {
    api.get('/catalog/categories').then(setCategories).catch(() => setCategories([]))
    api.get('/catalog/cities').then(setCities).catch(() => setCities([]))
  }
  useEffect(load, [])

  useEffect(() => {
    if (serviceCategoryId) api.get(`/catalog/categories/${serviceCategoryId}/services`).then(setServices).catch(() => setServices([]))
    else setServices([])
  }, [serviceCategoryId])

  useEffect(() => {
    if (areaCityId) api.get(`/catalog/cities/${areaCityId}/areas`).then(setAreas).catch(() => setAreas([]))
    else setAreas([])
  }, [areaCityId])

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

  async function createService() {
    if (!serviceCategoryId || !serviceName.trim()) return
    setError('')
    try {
      await api.post('/admin/catalog/services', { categoryId: Number(serviceCategoryId), name: serviceName, slug: slugify(serviceName) })
      setServiceName('')
      api.get(`/catalog/categories/${serviceCategoryId}/services`).then(setServices)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create service.')
    }
  }

  async function createArea() {
    if (!areaCityId || !areaName.trim()) return
    setError('')
    try {
      await api.post('/admin/catalog/areas', { cityId: Number(areaCityId), name: areaName })
      setAreaName('')
      api.get(`/catalog/cities/${areaCityId}/areas`).then(setAreas)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create area.')
    }
  }

  async function publishPolicy() {
    if (!policyVersion.trim() || !policyTitle.trim() || !policyContent.trim()) return
    setError('')
    try {
      await api.post('/admin/catalog/policies', { type: policyType, version: policyVersion, title: policyTitle, contentMarkdown: policyContent })
      setPolicyVersion(''); setPolicyTitle(''); setPolicyContent('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not publish policy.')
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

        <div>
          <strong>Services</strong>
          <div className="card" style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div className="field">
              <label>Category</label>
              <select value={serviceCategoryId} onChange={(e) => setServiceCategoryId(e.target.value)}>
                <option value="">Select…</option>
                {(categories || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {services.map((s) => <li key={s.id}>{s.name}</li>)}
            </ul>
            <TextField id="serviceName" label="New service name" value={serviceName} onChange={(e) => setServiceName(e.target.value)} />
            <Button onClick={createService} disabled={!serviceCategoryId}>Add service</Button>
          </div>
        </div>

        <div>
          <strong>Areas</strong>
          <div className="card" style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 10 }}>
            <div className="field">
              <label>City</label>
              <select value={areaCityId} onChange={(e) => setAreaCityId(e.target.value)}>
                <option value="">Select…</option>
                {(cities || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
            <ul style={{ margin: 0, paddingLeft: 18 }}>
              {areas.map((a) => <li key={a.id}>{a.name}</li>)}
            </ul>
            <TextField id="areaName" label="New area name" value={areaName} onChange={(e) => setAreaName(e.target.value)} />
            <Button onClick={createArea} disabled={!areaCityId}>Add area</Button>
          </div>
        </div>
      </div>

      <div style={{ marginTop: 20 }}>
        <strong>Publish a policy document</strong>
        <p className="subtitle" style={{ fontSize: 13 }}>Publishing retires the current version of the same type and makes this one current.</p>
        <div className="card" style={{ marginTop: 8, display: 'flex', flexDirection: 'column', gap: 10, maxWidth: 480 }}>
          <div className="field">
            <label>Type</label>
            <select value={policyType} onChange={(e) => setPolicyType(e.target.value)}>
              <option value="TERMS">Terms of Service</option>
              <option value="PRIVACY">Privacy Policy</option>
              <option value="MARKETING_CONSENT">Marketing Consent</option>
            </select>
          </div>
          <TextField id="policyVersion" label="Version (e.g. 2026-10)" value={policyVersion} onChange={(e) => setPolicyVersion(e.target.value)} />
          <TextField id="policyTitle" label="Title" value={policyTitle} onChange={(e) => setPolicyTitle(e.target.value)} />
          <TextField id="policyContent" label="Content (Markdown)" as="textarea" rows={6} value={policyContent} onChange={(e) => setPolicyContent(e.target.value)} />
          <Button onClick={publishPolicy}>Publish</Button>
        </div>
      </div>
    </div>
  )
}
