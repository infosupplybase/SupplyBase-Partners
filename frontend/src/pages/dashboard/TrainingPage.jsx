import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import { usePartner } from '../../context/PartnerContext'
import { useToast } from '../../context/ToastContext'

export default function TrainingPage() {
  const navigate = useNavigate()
  const { refresh } = usePartner()
  const showToast = useToast()
  const [courses, setCourses] = useState(null)
  const [enrollments, setEnrollments] = useState([])
  const [modules, setModules] = useState({})
  const [assessment, setAssessment] = useState(null)
  const [activeCourseId, setActiveCourseId] = useState(null)
  const [answers, setAnswers] = useState({})
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function loadEnrollments() {
    api.get('/training/enrollments').then(setEnrollments).catch(() => setEnrollments([]))
  }

  useEffect(() => {
    api.get('/training/courses').then((cs) => {
      setCourses(cs)
      if (cs.length > 0) setActiveCourseId(cs[0].id)
    }).catch(() => setCourses([]))
    loadEnrollments()
  }, [])

  useEffect(() => {
    if (!activeCourseId) return
    api.get(`/training/courses/${activeCourseId}/modules`).then((ms) =>
      setModules((prev) => ({ ...prev, [activeCourseId]: ms }))).catch(() => {})
  }, [activeCourseId])

  const enrollment = enrollments.find((e) => e.courseId === activeCourseId)
  const courseModules = modules[activeCourseId] || []

  async function completeModule(moduleId) {
    setBusy(true)
    try {
      await api.post(`/training/courses/${activeCourseId}/modules/${moduleId}/complete`)
      loadEnrollments()
      showToast('Lesson marked complete.')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not update progress.')
    } finally {
      setBusy(false)
    }
  }

  async function loadAssessment() {
    setError('')
    try {
      const q = await api.get(`/training/courses/${activeCourseId}/assessment`)
      setAssessment(q)
      setAnswers({})
      setResult(null)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No assessment available yet.')
    }
  }

  async function submitAssessment() {
    setBusy(true)
    setError('')
    try {
      const res = await api.post(`/training/courses/${activeCourseId}/assessment/submit`, answers)
      setResult(res)
      loadEnrollments()
      await refresh()
      if (res.passed) showToast('You passed the assessment!')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not submit your answers.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="screen">
      <div className="header" style={{ margin: '-20px -20px 0' }}>
        <button className="back-btn" onClick={() => navigate('/app/progress')} aria-label="Back">←</button>
        <h1>Training</h1>
      </div>

      {courses === null ? (
        <div className="skeleton" style={{ height: 160 }} />
      ) : courses.length === 0 ? (
        <div className="empty-state">No training courses are configured yet.</div>
      ) : (
        <>
          {courses.map((c) => (
            <div key={c.id} className="card" style={{ display: c.id === activeCourseId ? 'block' : 'none' }}>
              <strong>{c.title}</strong>
              <p className="subtitle" style={{ marginTop: 4 }}>{c.description}</p>
              <span className={`badge ${enrollment?.status === 'COMPLETED' ? 'badge-success' : 'badge-warning'}`}>
                {enrollment?.status?.replace('_', ' ') || 'Not started'}
              </span>
            </div>
          ))}

          <div className="selection-list">
            {courseModules.map((m) => (
              <div key={m.id} className="card" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={{ fontSize: 20 }}>{m.contentType === 'VIDEO' ? '🎬' : '📄'}</span>
                <div style={{ flex: 1 }}>
                  <div style={{ fontWeight: 700 }}>{m.title}</div>
                  {m.contentType === 'TEXT' ? (
                    <p className="subtitle" style={{ fontSize: 13 }}>{m.contentBody}</p>
                  ) : m.contentUrl ? (
                    <a href={m.contentUrl} target="_blank" rel="noreferrer">Watch video</a>
                  ) : (
                    <p className="subtitle" style={{ fontSize: 13 }}>Video unavailable right now &mdash; check back later.</p>
                  )}
                </div>
                <Button variant="secondary" disabled={busy} onClick={() => completeModule(m.id)}>Mark done</Button>
              </div>
            ))}
          </div>

          {!assessment ? (
            <Button block disabled={busy} onClick={loadAssessment}>Take the assessment</Button>
          ) : result ? (
            <div className="card" style={{ textAlign: 'center' }}>
              <div style={{ fontSize: 24, fontWeight: 800 }}>{result.scorePercent}%</div>
              <span className={`badge ${result.passed ? 'badge-success' : 'badge-danger'}`}>
                {result.passed ? 'Passed' : `Not passed — ${result.attemptsRemaining} attempt(s) left`}
              </span>
              {!result.passed && result.attemptsRemaining > 0 && (
                <div style={{ marginTop: 10 }}>
                  <Button variant="secondary" onClick={loadAssessment}>Try again</Button>
                </div>
              )}
            </div>
          ) : (
            <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              {assessment.map((q) => (
                <div key={q.id}>
                  <strong style={{ display: 'block', marginBottom: 8 }}>{q.questionText}</strong>
                  {q.options.map((opt) => (
                    <label key={opt.id} style={{ display: 'flex', gap: 8, marginBottom: 6 }}>
                      <input
                        type="radio" name={`q-${q.id}`}
                        checked={answers[q.id] === opt.id}
                        onChange={() => setAnswers((prev) => ({ ...prev, [q.id]: opt.id }))}
                      />
                      {opt.text}
                    </label>
                  ))}
                </div>
              ))}
              <Button disabled={busy} onClick={submitAssessment}>Submit answers</Button>
            </div>
          )}
        </>
      )}
      {error && <div className="field error">{error}</div>}
    </div>
  )
}
