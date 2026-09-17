import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'

export default function AdminTraining() {
  const [courses, setCourses] = useState(null)
  const [error, setError] = useState('')

  const [courseTitle, setCourseTitle] = useState('')
  const [courseDescription, setCourseDescription] = useState('')

  const [moduleCourseId, setModuleCourseId] = useState('')
  const [moduleTitle, setModuleTitle] = useState('')
  const [moduleBody, setModuleBody] = useState('')

  const [assessmentCourseId, setAssessmentCourseId] = useState('')
  const [assessmentTitle, setAssessmentTitle] = useState('')

  const [questionAssessmentId, setQuestionAssessmentId] = useState('')
  const [questionText, setQuestionText] = useState('')
  const [options, setOptions] = useState([{ text: '', correct: true }, { text: '', correct: false }])

  function load() {
    api.get('/training/courses').then(setCourses).catch(() => setCourses([]))
  }
  useEffect(load, [])

  async function createCourse() {
    if (!courseTitle.trim()) return
    setError('')
    try {
      await api.post('/admin/training/courses', { title: courseTitle, description: courseDescription, required: true })
      setCourseTitle(''); setCourseDescription('')
      load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create course.')
    }
  }

  async function addModule() {
    if (!moduleCourseId || !moduleTitle.trim()) return
    setError('')
    try {
      await api.post(`/admin/training/courses/${moduleCourseId}/modules`, { title: moduleTitle, contentType: 'TEXT', contentBody: moduleBody, displayOrder: 0 })
      setModuleTitle(''); setModuleBody('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not add module.')
    }
  }

  async function createAssessment() {
    if (!assessmentCourseId || !assessmentTitle.trim()) return
    setError('')
    try {
      await api.post(`/admin/training/courses/${assessmentCourseId}/assessment`, { title: assessmentTitle, passScorePercent: 70, maxAttempts: 3 })
      setAssessmentTitle('')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not create assessment.')
    }
  }

  function updateOption(index, field, value) {
    setOptions((prev) => prev.map((o, i) => (i === index ? { ...o, [field]: value } : field === 'correct' ? { ...o, correct: false } : o)))
  }

  async function addQuestion() {
    if (!questionAssessmentId || !questionText.trim() || options.some((o) => !o.text.trim())) return
    setError('')
    try {
      await api.post(`/admin/training/assessments/${questionAssessmentId}/questions`, {
        questionText, displayOrder: 0, options,
      })
      setQuestionText('')
      setOptions([{ text: '', correct: true }, { text: '', correct: false }])
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not add question.')
    }
  }

  return (
    <div>
      <h2 className="title">Training content</h2>
      {error && <div className="field error">{error}</div>}

      <table className="data-table" style={{ marginTop: 16, maxWidth: 500 }}>
        <thead><tr><th>ID</th><th>Course</th></tr></thead>
        <tbody>
          {(courses || []).map((c) => <tr key={c.id}><td>{c.id}</td><td>{c.title}</td></tr>)}
        </tbody>
      </table>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginTop: 20 }}>
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>New course</strong>
          <TextField id="courseTitle" label="Title" value={courseTitle} onChange={(e) => setCourseTitle(e.target.value)} />
          <TextField id="courseDesc" label="Description" as="textarea" rows={2} value={courseDescription} onChange={(e) => setCourseDescription(e.target.value)} />
          <Button onClick={createCourse}>Create course</Button>
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>Add module</strong>
          <TextField id="moduleCourseId" label="Course ID" value={moduleCourseId} onChange={(e) => setModuleCourseId(e.target.value)} />
          <TextField id="moduleTitle" label="Module title" value={moduleTitle} onChange={(e) => setModuleTitle(e.target.value)} />
          <TextField id="moduleBody" label="Lesson text" as="textarea" rows={3} value={moduleBody} onChange={(e) => setModuleBody(e.target.value)} />
          <Button onClick={addModule}>Add module</Button>
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>Create assessment</strong>
          <TextField id="assessCourseId" label="Course ID" value={assessmentCourseId} onChange={(e) => setAssessmentCourseId(e.target.value)} />
          <TextField id="assessTitle" label="Assessment title" value={assessmentTitle} onChange={(e) => setAssessmentTitle(e.target.value)} />
          <Button onClick={createAssessment}>Create assessment</Button>
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <strong>Add question</strong>
          <TextField id="questionAssessmentId" label="Assessment ID" value={questionAssessmentId} onChange={(e) => setQuestionAssessmentId(e.target.value)} />
          <TextField id="questionText" label="Question" value={questionText} onChange={(e) => setQuestionText(e.target.value)} />
          {options.map((opt, i) => (
            <div key={i} style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <input
                style={{ flex: 1, border: '1px solid var(--color-border)', borderRadius: 8, padding: 8 }}
                value={opt.text} placeholder={`Option ${i + 1}`}
                onChange={(e) => updateOption(i, 'text', e.target.value)}
              />
              <label style={{ display: 'flex', gap: 4, fontSize: 13 }}>
                <input type="radio" name="correctOption" checked={opt.correct} onChange={() => updateOption(i, 'correct', true)} /> Correct
              </label>
            </div>
          ))}
          <button className="btn btn-ghost" style={{ alignSelf: 'flex-start' }} onClick={() => setOptions((prev) => [...prev, { text: '', correct: false }])}>+ Add option</button>
          <Button onClick={addQuestion}>Add question</Button>
        </div>
      </div>
    </div>
  )
}
