import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getProfile, updateProfileName } from '../api/api.js';
import useAsync from '../hooks/useAsync.js';
import { EmptyState, ErrorState, SkeletonList } from '../components/States.jsx';
import { formatDateTime } from '../utils/format.js';

export default function ProfilePage() {
  const { data: profile, loading, error, reload, setData } = useAsync(getProfile, []);
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState('');
  const [saveError, setSaveError] = useState('');

  useEffect(() => {
    if (profile?.name) setName(profile.name);
  }, [profile?.name]);

  async function saveName(event) {
    event.preventDefault();
    if (!name.trim() || saving) return;
    setSaving(true);
    setMessage('');
    setSaveError('');
    try {
      const updated = await updateProfileName(name.trim());
      setData(updated);
      setMessage('Profile updated.');
    } catch (err) {
      setSaveError(err?.response?.data?.message || 'Could not update your name.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <SkeletonList rows={4} />;
  if (error) return <ErrorState message={error} onRetry={reload} />;

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h1>Profile</h1>
          <p className="muted">Your learning record is stored in PostgreSQL.</p>
        </div>
      </div>

      <section className="card profile-card">
        <div className="avatar" aria-hidden="true">{profile.name?.slice(0, 1).toUpperCase()}</div>
        <div>
          <h2>{profile.name}</h2>
          <p className="muted small">{profile.email}</p>
        </div>
      </section>

      <section className="stat-grid">
        <div className="stat card">
          <strong>{profile.streakDays}</strong>
          <span>day streak</span>
        </div>
        <div className="stat card">
          <strong>{profile.articlesRead}</strong>
          <span>articles read</span>
        </div>
        <div className="stat card">
          <strong>{profile.completedTopics}/{profile.totalTopics}</strong>
          <span>topics done</span>
        </div>
        <div className="stat card">
          <strong>{profile.bookmarks}</strong>
          <span>bookmarks</span>
        </div>
      </section>

      <section className="card">
        <h2>Display name</h2>
        <form className="inline-form" onSubmit={saveName}>
          <label className="field">
            <span>Name</span>
            <input value={name} onChange={(event) => setName(event.target.value)} maxLength={120} required />
          </label>
          <button type="submit" className="button" disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </form>
        {message && <p className="notice success">{message}</p>}
        {saveError && <p className="notice error">{saveError}</p>}
      </section>

      <section>
        <div className="section-header">
          <h2>Reading history</h2>
          <span className="muted small">{profile.planProgressPercent}% of daily plan</span>
        </div>
        {!profile.history?.length ? (
          <EmptyState title="No reading history" message="Open an article and it will appear here." />
        ) : (
          <div className="stack tight">
            {profile.history.map((item) => (
              <article className="card history-card" key={item.id}>
                <Link to={`/news/${item.articleId}`} className="card-link">
                  <h3>{item.title}</h3>
                </Link>
                <p className="muted small">
                  {item.source} · read {formatDateTime(item.readAt)}
                </p>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
