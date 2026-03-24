import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const fetchPrescriptions = createAsyncThunk('prescription/fetchPrescriptions', async () => (await api.get('/api/doctor/prescriptions')).data.data);
export const createPrescription = createAsyncThunk('prescription/createPrescription', async (payload) => (await api.post('/api/doctor/prescriptions', payload)).data.data);
const slice = createSlice({
  name: 'prescription',
  initialState: { items: [], loading: false, error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchPrescriptions.pending, (s) => { s.loading = true; })
     .addCase(fetchPrescriptions.fulfilled, (s, a) => { s.loading = false; s.items = Array.isArray(a.payload) ? a.payload : [a.payload]; })
     .addCase(fetchPrescriptions.rejected, (s, a) => { s.loading = false; s.error = a.error.message; });
    b.addCase(createPrescription.pending, (s) => { s.loading = true; s.error = null; })
     .addCase(createPrescription.fulfilled, (s, a) => {
       s.loading = false;
       s.items = [a.payload, ...s.items];
     })
     .addCase(createPrescription.rejected, (s, a) => {
       s.loading = false;
       s.error = a.error.message;
     });
  }
});
export default slice.reducer;
