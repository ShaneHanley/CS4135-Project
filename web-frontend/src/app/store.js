import { configureStore } from '@reduxjs/toolkit';
import auth from '../features/authSlice';
import prescription from '../features/prescriptionSlice';
import pharmacy from '../features/pharmacySlice';
import patient from '../features/patientSlice';
import inventory from '../features/inventorySlice';
import billing from '../features/billingSlice';
import analytics from '../features/analyticsSlice';
export const store = configureStore({ reducer: { auth, prescription, pharmacy, patient, inventory, billing, analytics } });
