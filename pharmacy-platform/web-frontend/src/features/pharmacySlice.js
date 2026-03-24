import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import api from '../api/client';

export const fetchPharmacyPrescriptions = createAsyncThunk(
  'pharmacy/fetchPrescriptions',
  async () => (await api.get('/api/pharmacy/prescriptions')).data.data
);

export const updatePharmacyPrescriptionStatus = createAsyncThunk(
  'pharmacy/updateStatus',
  async ({ id, status, rejectionReason }) =>
    (await api.put(`/api/pharmacy/prescriptions/${id}/status`, { status, rejectionReason })).data.data
);

const pharmacySlice = createSlice({
  name: 'pharmacy',
  initialState: { items: [], loading: false, error: null },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchPharmacyPrescriptions.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchPharmacyPrescriptions.fulfilled, (state, action) => {
        state.loading = false;
        state.items = Array.isArray(action.payload) ? action.payload : [];
      })
      .addCase(fetchPharmacyPrescriptions.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      })
      .addCase(updatePharmacyPrescriptionStatus.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(updatePharmacyPrescriptionStatus.fulfilled, (state, action) => {
        state.loading = false;
        const updated = action.payload;
        const id = updated.prescriptionId;
        state.items = state.items.map((item) => (String(item.prescriptionId) === String(id) ? updated : item));
      })
      .addCase(updatePharmacyPrescriptionStatus.rejected, (state, action) => {
        state.loading = false;
        state.error = action.error.message;
      });
  }
});

export default pharmacySlice.reducer;
