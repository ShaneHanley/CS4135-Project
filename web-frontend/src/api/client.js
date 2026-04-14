import axios from 'axios';
const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
const api = axios.create({ baseURL });
api.interceptors.request.use((config) => {
  const raw = localStorage.getItem('auth');
  if (raw) {
    const auth = JSON.parse(raw);
    if (auth?.accessToken) config.headers.Authorization = `Bearer ${auth.accessToken}`;
  }
  return config;
});
api.interceptors.response.use((res) => res, async (error) => {
  if (error.response?.status !== 401) throw error;
  const raw = localStorage.getItem('auth');
  if (!raw) throw error;
  const auth = JSON.parse(raw);
  try {
    const res = await axios.post(`${baseURL}/api/auth/refresh`, { refreshToken: auth.refreshToken });
    localStorage.setItem('auth', JSON.stringify(res.data.data));
    error.config.headers.Authorization = `Bearer ${res.data.data.accessToken}`;
    return axios(error.config);
  } catch (_e) {
    localStorage.removeItem('auth');
    window.location.href = '/login';
    throw error;
  }
});
export default api;
