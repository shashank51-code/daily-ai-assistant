import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="state empty-state">
      <strong>Page not found</strong>
      <p>The screen you opened does not exist.</p>
      <Link className="button" to="/">
        Back to home
      </Link>
    </div>
  );
}
