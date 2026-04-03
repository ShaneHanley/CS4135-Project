import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const fetchInvoices = createAsyncThunk('billing/fetchInvoices', async () => (await api.get('/api/billing/invoices')).data.data);
const slice = createSlice({
  name: 'billing',
  initialState: { items: [], loading: false, error: null },
  reducers: {},
  extraReducers: (b) => {
    b.addCase(fetchInvoices.pending, (s) => { s.loading = true; })
     .addCase(fetchInvoices.fulfilled, (s, a) => { s.loading = false; s.items = Array.isArray(a.payload) ? a.payload : [a.payload]; })
     .addCase(fetchInvoices.rejected, (s, a) => { s.loading = false; s.error = a.error.message; });
  }
});
export default slice.reducer;
