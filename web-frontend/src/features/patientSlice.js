import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const fetchPatientProfile = createAsyncThunk('patient/fetchPatientProfile', async () => (await api.get('/api/patients/me')).data.data);
const slice = createSlice({
  name: 'patient',
  initialState: { items: [], loading: false, error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchPatientProfile.pending, (s) => { s.loading = true; })
     .addCase(fetchPatientProfile.fulfilled, (s, a) => { s.loading = false; s.items = Array.isArray(a.payload) ? a.payload : [a.payload]; })
     .addCase(fetchPatientProfile.rejected, (s, a) => { s.loading = false; s.error = a.error.message; });
  }
});
export default slice.reducer;
