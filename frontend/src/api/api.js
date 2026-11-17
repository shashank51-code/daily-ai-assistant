import client from './client';

export const getDashboard = () => client.get('/api/dashboard').then((r) => r.data);

export const getNews = (params = {}) => client.get('/api/news', { params }).then((r) => r.data);
export const getLatestNews = (limit = 5) =>
  client.get('/api/news/latest', { params: { limit } }).then((r) => r.data);
export const getArticle = (id) => client.get(`/api/news/${id}`).then((r) => r.data);
export const searchNews = (params) =>
  client.get('/api/news/search', { params }).then((r) => r.data);
export const getCategories = () => client.get('/api/categories').then((r) => r.data);

export const getRoadmap = () => client.get('/api/learning/roadmap').then((r) => r.data);
export const getTopic = (id) => client.get(`/api/learning/topics/${id}`).then((r) => r.data);
export const updateProgress = (topicId, status) =>
  client.post('/api/learning/progress', { topicId, status }).then((r) => r.data);
export const getDailyPlan = () => client.get('/api/learning/daily-plan').then((r) => r.data);
export const completeDailyTask = (taskId, completed = true) =>
  client
    .post(`/api/learning/daily-plan/${taskId}`, null, { params: { completed } })
    .then((r) => r.data);

export const getTools = (params = {}) => client.get('/api/tools', { params }).then((r) => r.data);
export const getTool = (id) => client.get(`/api/tools/${id}`).then((r) => r.data);
export const getToolCategories = () => client.get('/api/tools/categories').then((r) => r.data);

export const getBookmarks = (itemType) =>
  client.get('/api/bookmarks', { params: itemType ? { itemType } : {} }).then((r) => r.data);
export const addBookmark = (itemType, itemId) =>
  client.post('/api/bookmarks', { itemType, itemId }).then((r) => r.data);
export const removeBookmark = (itemType, itemId) =>
  client.delete('/api/bookmarks', { params: { itemType, itemId } });

export const getProfile = () => client.get('/api/profile').then((r) => r.data);
export const updateProfileName = (name) =>
  client.put('/api/profile', { name }).then((r) => r.data);
export const getHistory = (limit = 20) =>
  client.get('/api/profile/history', { params: { limit } }).then((r) => r.data);

export const refreshNews = () => client.post('/api/admin/news/refresh').then((r) => r.data);
