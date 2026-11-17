import { Outlet } from 'react-router-dom';
import BottomNav from './BottomNav.jsx';
import { MoonIcon, SunIcon } from './Icons.jsx';

export default function Layout({ theme, onToggleTheme }) {
  return (
    <div className="app-shell">
      <header className="top-bar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            AI
          </span>
          <div>
            <strong>Daily AI</strong>
            <span className="brand-subtitle">Updates &amp; learning</span>
          </div>
        </div>
        <button
          type="button"
          className="icon-button"
          onClick={onToggleTheme}
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
          title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
        >
          {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
        </button>
      </header>

      <main className="page-content">
        <Outlet />
      </main>

      <BottomNav />
    </div>
  );
}
