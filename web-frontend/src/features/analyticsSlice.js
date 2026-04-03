import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const fetchVolume = createAsyncThunk('analytics/volume', async (params) => (await api.get('/api/analytics/prescriptions/volume', { params })).data.data);
export const fetchRevenue = createAsyncThunk('analytics/revenue', async (params) => (await api.get('/api/analytics/revenue', { params })).data.data);
export const fetchKPIs = createAsyncThunk('analytics/kpi', async (params) => (await api.get('/api/analytics/kpi', { params })).data.data);
const slice = createSlice({
  name: 'analytics',
  initialState: { volume: null, revenue: null, kpi: null, loading: false, error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchVolume.pending, (s) => { s.loading = true; })
     .addCase(fetchVolume.fulfilled, (s, a) => { s.loading = false; s.volume = a.payload; })
     .addCase(fetchRevenue.fulfilled, (s, a) => { s.revenue = a.payload; })
     .addCase(fetchKPIs.fulfilled, (s, a) => { s.kpi = a.payload; });
  }
});
export default slice.reducer;
