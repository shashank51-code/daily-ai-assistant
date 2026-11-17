import { useState } from 'react';
import { Link } from 'react-router-dom';
import { getBookmarks, removeBookmark } from '../api/api.js';
import useAsync from '../hooks/useAsync.js';
import { EmptyState, ErrorState, SkeletonList } from '../components/States.jsx';
import { BookmarkIcon, ExternalIcon } from '../components/Icons.jsx';
import { formatDate } from '../utils/format.js';

const FILTERS = [
  ['ALL', 'All'],
  ['ARTICLE', 'Articles'],
  ['TOOL', 'Tools'],
  ['TOPIC', 'Topics'],
];

export default function BookmarksPage() {
  const [filter, setFilter] = useState('ALL');
  const { data, loading, error, reload, setData } = useAsync(
    () => getBookmarks(filter === 'ALL' ? undefined : filter),
    [filter]
  );
  const [removing, setRemoving] = useState(null);

  async function remove(bookmark) {
    if (removing) return;
    setRemoving(bookmark.id);
    try {
      await removeBookmark(bookmark.itemType, bookmark.itemId);
      setData((data || []).filter((item) => item.id !== bookmark.id));
    } finally {
      setRemoving(null);
    }
  }

  return (
    <div className="stack">
      <div className="page-title">
        <div>
          <h1>Saved items</h1>
          <p className="muted">Articles, tools and learning topics you kept for later.</p>
        </div>
      </div>

      <div className="segmented" role="tablist" aria-label="Filter bookmarks">
        {FILTERS.map(([value, label]) => (
          <button
            key={value}
            type="button"
            role="tab"
            aria-selected={filter === value}
            className={filter === value ? 'active' : ''}
            onClick={() => setFilter(value)}
          >
            {label}
          </button>
        ))}
      </div>

      {loading && <SkeletonList rows={3} />}
      {error && <ErrorState message={error} onRetry={reload} />}
      {!loading && !error && !data?.length && (
        <EmptyState
          title="Nothing saved yet"
          message="Tap the bookmark icon on an article, tool or topic to save it here."
        />
      )}

      {!loading && !error && data?.length > 0 && (
        <div className="stack tight">
          {data.map((bookmark) => {
            const target =
              bookmark.itemType === 'ARTICLE'
                ? `/news/${bookmark.itemId}`
                : bookmark.itemType === 'TOPIC'
                  ? `/learn/${bookmark.itemId}`
                  : null;

            return (
              <article className="card saved-card" key={bookmark.id}>
                <div className="article-card-top">
                  <span className="pill">{bookmark.itemType.toLowerCase()}</span>
                  <button
                    type="button"
                    className="bookmark-toggle saved"
                    onClick={() => remove(bookmark)}
                    disabled={removing === bookmark.id}
                    aria-label="Remove bookmark"
                    title="Remove bookmark"
                  >
                    <BookmarkIcon filled width={19} height={19} />
                  </button>
                </div>

                {target ? (
                  <Link to={target} className="card-link">
                    <h3>{bookmark.title}</h3>
                  </Link>
                ) : (
                  <h3>{bookmark.title}</h3>
                )}
                <p className="muted small">{bookmark.subtitle}</p>

                <div className="card-meta">
                  <span>Saved {formatDate(bookmark.createdAt)}</span>
                  {bookmark.url && bookmark.itemType === 'ARTICLE' && (
                    <Link className="read-more" to={`/news/${bookmark.itemId}`}>Read more</Link>
                  )}
                  {bookmark.url && bookmark.itemType === 'TOOL' && (
                    <a className="text-link" href={bookmark.url} target="_blank" rel="noreferrer">
                      Official site <ExternalIcon width={14} height={14} />
                    </a>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
