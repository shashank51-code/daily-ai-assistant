import { useEffect, useState } from 'react';
import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import HomePage from './pages/HomePage.jsx';
import DiscoverPage from './pages/DiscoverPage.jsx';
import ArticlePage from './pages/ArticlePage.jsx';
import LearnPage from './pages/LearnPage.jsx';
import TopicPage from './pages/TopicPage.jsx';
import BookmarksPage from './pages/BookmarksPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';
import NotFoundPage from './pages/NotFoundPage.jsx';

function initialTheme() {
  const stored = localStorage.getItem('dailyai-theme');
  if (stored === 'dark' || stored === 'light') return stored;
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export default function App() {
  const [theme, setTheme] = useState(initialTheme);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem('dailyai-theme', theme);
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', theme === 'dark' ? '#0b1220' : '#f7f8fb');
  }, [theme]);

  return (
    <Routes>
      <Route element={<Layout theme={theme} onToggleTheme={() => setTheme(theme === 'dark' ? 'light' : 'dark')} />}>
        <Route index element={<HomePage />} />
        <Route path="discover" element={<DiscoverPage />} />
        <Route path="news/:id" element={<ArticlePage />} />
        <Route path="learn" element={<LearnPage />} />
        <Route path="learn/:id" element={<TopicPage />} />
        <Route path="bookmarks" element={<BookmarksPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
