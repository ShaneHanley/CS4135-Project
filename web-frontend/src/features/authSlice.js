import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const login = createAsyncThunk('auth/login', async (payload) => (await api.post('/api/auth/login', payload)).data.data);
export const register = createAsyncThunk('auth/register', async (payload) => (await api.post('/api/auth/register', payload)).data.data);
export const refreshToken = createAsyncThunk('auth/refresh', async (payload) => (await api.post('/api/auth/refresh', payload)).data.data);
export const fetchMe = createAsyncThunk('auth/me', async () => (await api.get('/api/auth/me')).data.data);
const persisted = (() => {
  try {
    const raw = localStorage.getItem('auth');
    return raw ? JSON.parse(raw) : null;
  } catch (_e) {
    return null;
  }
})();
const initial = {
  accessToken: persisted?.accessToken ?? null,
  refreshToken: persisted?.refreshToken ?? null,
  user: persisted?.user ?? null,
  loading: false,
  error: null
};
const slice = createSlice({
  name: 'auth', initialState: initial,
  reducers: {
    logout(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      state.loading = false;
      state.error = null;
      localStorage.removeItem('auth');
    }
  },
  extraReducers: (b) => {
    b.addCase(login.pending, (s) => { s.loading = true; s.error = null; })
     .addCase(login.fulfilled, (s, a) => { s.loading = false; Object.assign(s, a.payload); localStorage.setItem('auth', JSON.stringify(a.payload)); })
     .addCase(login.rejected, (s, a) => { s.loading = false; s.error = a.error.message; })
     .addCase(refreshToken.fulfilled, (s, a) => { Object.assign(s, a.payload); localStorage.setItem('auth', JSON.stringify(a.payload)); })
     .addCase(fetchMe.fulfilled, (s, a) => { s.user = a.payload; });
  }
});
export const { logout } = slice.actions;
export default slice.reducer;
