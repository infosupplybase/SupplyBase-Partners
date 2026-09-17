import { useEffect, useState } from 'react'
import { api, ApiError } from '../../api/client'
import Button from '../../components/Button'
import TextField from '../../components/TextField'
import { useToast } from '../../context/ToastContext'

export default function CommunityTab() {
  const showToast = useToast()
  const [announcements, setAnnouncements] = useState(null)
  const [posts, setPosts] = useState(null)
  const [draft, setDraft] = useState('')
  const [openComments, setOpenComments] = useState(null)
  const [comments, setComments] = useState([])
  const [commentDraft, setCommentDraft] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  function loadPosts() {
    api.get('/community/posts').then((page) => setPosts(page.content)).catch(() => setPosts([]))
  }

  useEffect(() => {
    api.get('/community/announcements').then(setAnnouncements).catch(() => setAnnouncements([]))
    loadPosts()
  }, [])

  async function createPost() {
    if (!draft.trim()) return
    setBusy(true)
    setError('')
    try {
      await api.post('/community/posts', { body: draft })
      setDraft('')
      loadPosts()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not post.')
    } finally {
      setBusy(false)
    }
  }

  async function openPostComments(postId) {
    setOpenComments(postId)
    const cs = await api.get(`/community/posts/${postId}/comments`).catch(() => [])
    setComments(cs)
  }

  async function submitComment(postId) {
    if (!commentDraft.trim()) return
    await api.post(`/community/posts/${postId}/comments`, { body: commentDraft }).catch(() => {})
    setCommentDraft('')
    openPostComments(postId)
  }

  async function reportPost(postId) {
    await api.post('/community/reports', { targetType: 'POST', targetId: postId, reason: 'Reported by partner' }).catch(() => {})
    showToast('Reported to moderators.')
  }

  return (
    <div className="screen">
      <h2 className="title">Community</h2>

      {announcements && announcements.length > 0 && (
        <div>
          <strong>Announcements</strong>
          <div className="selection-list" style={{ marginTop: 8 }}>
            {announcements.map((a) => (
              <div key={a.id} className="card" style={{ background: 'var(--color-accent-soft)', border: 'none' }}>
                <strong>{a.title}</strong>
                <p className="subtitle" style={{ marginTop: 4 }}>{a.body}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <TextField id="draft" label="Share something with other partners" as="textarea" rows={2}
          value={draft} onChange={(e) => setDraft(e.target.value)} />
        {error && <div className="field error">{error}</div>}
        <Button disabled={busy || !draft.trim()} onClick={createPost}>Post</Button>
      </div>

      <div>
        <strong>Partner posts</strong>
        <div className="selection-list" style={{ marginTop: 8 }}>
          {posts === null ? (
            <div className="skeleton" style={{ height: 80 }} />
          ) : posts.length === 0 ? (
            <div className="empty-state">No posts yet &mdash; be the first to share!</div>
          ) : posts.map((p) => (
            <div key={p.id} className="card">
              <p style={{ margin: 0 }}>{p.body}</p>
              <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
                <button className="btn-ghost btn" style={{ minHeight: 'auto', padding: 0 }} onClick={() => openPostComments(p.id)}>Comments</button>
                <button className="btn-ghost btn" style={{ minHeight: 'auto', padding: 0, color: 'var(--color-danger)' }} onClick={() => reportPost(p.id)}>Report</button>
              </div>
              {openComments === p.id && (
                <div style={{ marginTop: 10, borderTop: '1px solid var(--color-border)', paddingTop: 10 }}>
                  {comments.map((c) => (
                    <p key={c.id} className="subtitle" style={{ margin: '4px 0' }}>{c.body}</p>
                  ))}
                  <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
                    <input
                      style={{ flex: 1, border: '1px solid var(--color-border)', borderRadius: 8, padding: 8 }}
                      value={commentDraft} onChange={(e) => setCommentDraft(e.target.value)} placeholder="Write a comment"
                    />
                    <Button variant="secondary" onClick={() => submitComment(p.id)}>Send</Button>
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
