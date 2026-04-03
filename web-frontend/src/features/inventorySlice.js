import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const fetchMedications = createAsyncThunk('inventory/fetchMedications', async () => (await api.get('/api/inventory/medications')).data.data);
const slice = createSlice({
  name: 'inventory',
  initialState: { items: [], loading: false, error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchMedications.pending, (s) => { s.loading = true; })
     .addCase(fetchMedications.fulfilled, (s, a) => { s.loading = false; s.items = Array.isArray(a.payload) ? a.payload : [a.payload]; })
     .addCase(fetchMedications.rejected, (s, a) => { s.loading = false; s.error = a.error.message; });
  }
});
export default slice.reducer;
