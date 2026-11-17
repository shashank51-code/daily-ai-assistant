import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getTopic, updateProgress } from '../api/api.js';
import useAsync from '../hooks/useAsync.js';
import BookmarkToggle from '../components/BookmarkToggle.jsx';
import { EmptyState, ErrorState, SkeletonList } from '../components/States.jsx';
import { ArrowLeftIcon, CheckIcon } from '../components/Icons.jsx';
import { STATUS_LABELS } from '../utils/format.js';

const LISTS = [
  ['prerequisites', 'Prerequisites'],
  ['resources', 'Learning resources'],
  ['exercises', 'Practical exercises'],
  ['projectIdeas', 'Mini-project ideas'],
  ['interviewQuestions', 'Interview questions'],
];

const STATUSES = ['NOT_STARTED', 'IN_PROGRESS', 'COMPLETED'];

export default function TopicPage() {
  const { id } = useParams();
  const { data: topic, loading, error, reload, setData } = useAsync(() => getTopic(id), [id]);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState('');

  async function changeStatus(status) {
    if (saving || !topic || topic.status === status) return;
    setSaving(true);
    setSaveError('');
    try {
      const progress = await updateProgress(topic.id, status);
      setData({ ...topic, status: progress.status, completedAt: progress.completedAt });
    } catch {
      setSaveError('Could not save your progress. Try again.');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <SkeletonList rows={5} />;
  if (error) return <ErrorState message={error} onRetry={reload} />;
  if (!topic) return <EmptyState title="Topic not found" />;

  return (
    <div className="stack">
      <Link to="/learn" className="back-link">
        <ArrowLeftIcon width={18} height={18} /> Back to roadmap
      </Link>

      <header className="card topic-header">
        <div className="article-card-top">
          <span className="pill">Level {topic.level} · {topic.levelName}</span>
          <BookmarkToggle itemType="TOPIC" itemId={topic.id} initial={false} />
        </div>
        <h1>{topic.title}</h1>
        <p className="muted small">Current status: {STATUS_LABELS[topic.status] || topic.status}</p>
        <div className="button-row">
          {STATUSES.map((status) => (
            <button
              key={status}
              type="button"
              className={`button small ${topic.status === status ? '' : 'ghost'}`}
              onClick={() => changeStatus(status)}
              disabled={saving}
            >
              {status === 'COMPLETED' && <CheckIcon width={15} height={15} />}
              {STATUS_LABELS[status]}
            </button>
          ))}
        </div>
        {saveError && <p className="notice error">{saveError}</p>}
      </header>

      {topic.explanation && (
        <section className="card topic-section">
          <h2>Explanation</h2>
          {topic.explanation.split(/\n+/).filter(Boolean).map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
        </section>
      )}

      {LISTS.map(([key, label]) => {
        const items = topic[key] || [];
        if (!items.length) return null;
        return (
          <section className="card topic-section" key={key}>
            <h2>{label}</h2>
            <ul className={key === 'resources' ? 'link-list' : 'bullet-list'}>
              {items.map((item) => (
                <li key={item}>{renderItem(item)}</li>
              ))}
            </ul>
          </section>
        );
      })}
    </div>
  );
}

function renderItem(item) {
  const match = item.match(/^(.*?)\s+-\s+(https?:\/\/\S+)$/);
  if (!match) return item;
  return (
    <>
      {match[1]} —{' '}
      <a className="text-link" href={match[2]} target="_blank" rel="noreferrer">
        open resource
      </a>
    </>
  );
}
