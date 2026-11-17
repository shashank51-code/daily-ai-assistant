import { useState } from 'react';
import BookmarkToggle from './BookmarkToggle.jsx';
import { ExternalIcon } from './Icons.jsx';

export default function ToolCard({ tool }) {
  const [expanded, setExpanded] = useState(false);

  return (
    <article className="card tool-card">
      <div className="article-card-top">
        <span className="pill">{tool.category}</span>
        <BookmarkToggle itemType="TOOL" itemId={tool.id} initial={tool.bookmarked} />
      </div>

      <h3>{tool.name}</h3>
      {tool.description && <p className="card-description">{tool.description}</p>}

      {expanded && (
        <div className="tool-details">
          {tool.features?.length > 0 && (
            <div>
              <h4>Main features</h4>
              <ul className="feature-list">
                {tool.features.map((feature) => (
                  <li key={feature}>{feature}</li>
                ))}
              </ul>
            </div>
          )}
          {tool.pricing && (
            <p>
              <strong>Pricing:</strong> {tool.pricing}
            </p>
          )}
          {tool.freeAlternatives && (
            <p>
              <strong>Free alternatives:</strong> {tool.freeAlternatives}
            </p>
          )}
          {tool.pricingVerifiedOn && (
            <p className="muted small">Pricing notes checked on {tool.pricingVerifiedOn}.</p>
          )}
        </div>
      )}

      <div className="card-meta tool-actions">
        {tool.website && (
          <a href={tool.website} target="_blank" rel="noreferrer" className="text-link">
            Official site <ExternalIcon width={15} height={15} />
          </a>
        )}
        <button type="button" className="text-button" onClick={() => setExpanded((value) => !value)}>
          {expanded ? 'Show less' : 'Details'}
        </button>
      </div>
    </article>
  );
}
