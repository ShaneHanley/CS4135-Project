from pathlib import Path

ROOT = Path(r"c:\Users\josep\cs4135-copy\pharmacy-platform\web-frontend")


def w(p: Path, c: str):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(c.strip() + "\n", encoding="utf-8")


w(
    ROOT / "src/api/client.js",
    """
import axios from 'axios';
const baseURL = import.meta.env.REACT_APP_API_BASE_URL || 'http://localhost:8080';
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
""",
)
w(
    ROOT / "src/app/store.js",
    """
import { configureStore } from '@reduxjs/toolkit';
import auth from '../features/authSlice';
import prescription from '../features/prescriptionSlice';
import patient from '../features/patientSlice';
import inventory from '../features/inventorySlice';
import billing from '../features/billingSlice';
import analytics from '../features/analyticsSlice';
export const store = configureStore({ reducer: { auth, prescription, patient, inventory, billing, analytics } });
""",
)
w(
    ROOT / "src/features/authSlice.js",
    """
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import api from '../api/client';
export const login = createAsyncThunk('auth/login', async (payload) => (await api.post('/api/auth/login', payload)).data.data);
export const register = createAsyncThunk('auth/register', async (payload) => (await api.post('/api/auth/register', payload)).data.data);
export const refreshToken = createAsyncThunk('auth/refresh', async (payload) => (await api.post('/api/auth/refresh', payload)).data.data);
export const fetchMe = createAsyncThunk('auth/me', async () => (await api.get('/api/auth/me')).data.data);
const initial = { accessToken: null, refreshToken: null, user: null, loading: false, error: null };
const slice = createSlice({
  name: 'auth', initialState: initial,
  reducers: { logout(state){ Object.assign(state, initial); localStorage.removeItem('auth'); } },
  extraReducers: (b) => {
    b.addCase(login.pending, (s) => { s.loading = true; s.error = null; })
     .addCase(login.fulfilled, (s, a) => { s.loading = false; Object.assign(s, a.payload); localStorage.setItem('auth', JSON.stringify(a.payload)); })
     .addCase(login.rejected, (s, a) => { s.loading = false; s.error = a.error.message; })
     .addCase(fetchMe.fulfilled, (s, a) => { s.user = a.payload; });
  }
});
export const { logout } = slice.actions;
export default slice.reducer;
""",
)

for name, ep, thunk in [
    ("prescription", "/api/doctor/prescriptions", "fetchPrescriptions"),
    ("patient", "/api/patients/me", "fetchPatientProfile"),
    ("inventory", "/api/inventory/medications", "fetchMedications"),
    ("billing", "/api/billing/invoices", "fetchInvoices"),
]:
    w(
        ROOT / f"src/features/{name}Slice.js",
        f"""
import {{ createSlice, createAsyncThunk }} from '@reduxjs/toolkit';
import api from '../api/client';
export const {thunk} = createAsyncThunk('{name}/{thunk}', async () => (await api.get('{ep}')).data.data);
const slice = createSlice({{
  name: '{name}',
  initialState: {{ items: [], loading: false, error: null }},
  reducers: {{}},
  extraReducers: (b) => {{
    b.addCase({thunk}.pending, (s) => {{ s.loading = true; }})
     .addCase({thunk}.fulfilled, (s, a) => {{ s.loading = false; s.items = Array.isArray(a.payload) ? a.payload : [a.payload]; }})
     .addCase({thunk}.rejected, (s, a) => {{ s.loading = false; s.error = a.error.message; }});
  }}
}});
export default slice.reducer;
""",
    )
w(
    ROOT / "src/features/analyticsSlice.js",
    """
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
""",
)
w(ROOT / "src/routes/ProtectedRoute.jsx", "import { Navigate } from 'react-router-dom';import { useSelector } from 'react-redux';export default function ProtectedRoute({children}){ return useSelector(s=>s.auth.accessToken) ? children : <Navigate to='/login' replace/>; }")
w(ROOT / "src/routes/RoleRoute.jsx", "import { Navigate } from 'react-router-dom';import { useSelector } from 'react-redux';export default function RoleRoute({roles, children}){ const role = useSelector(s=>s.auth.user?.role); return roles.includes(role) ? children : <Navigate to='/unauthorized' replace/>; }")
w(ROOT / "src/hooks/useAuth.js", "import { useSelector } from 'react-redux'; export const useAuth = () => useSelector((s) => s.auth);")
w(ROOT / "src/hooks/useAppDispatch.js", "import { useDispatch } from 'react-redux'; export const useAppDispatch = useDispatch;")
w(ROOT / "src/hooks/useAppSelector.js", "import { useSelector } from 'react-redux'; export const useAppSelector = useSelector;")
w(ROOT / "src/components/Navbar.jsx", "export default function Navbar(){ return <nav>Pharmacy Platform</nav>; }")
w(ROOT / "src/components/Sidebar.jsx", "export default function Sidebar(){ return <aside>Menu</aside>; }")
w(ROOT / "src/components/Footer.jsx", "export default function Footer(){ return <footer>Footer</footer>; }")
w(ROOT / "src/components/LoadingSpinner.jsx", "export default function LoadingSpinner(){ return <div>Loading...</div>; }")
w(ROOT / "src/components/ErrorBanner.jsx", "export default function ErrorBanner({message}){ return message ? <div style={{color:'red'}}>{message}</div> : null; }")
w(ROOT / "src/pages/LoginPage.jsx", "export default function LoginPage(){ return <div>Login Page</div>; }")
w(ROOT / "src/pages/PatientPage.jsx", "export default function PatientPage(){ return <div>Patient Dashboard</div>; }")
w(ROOT / "src/pages/DoctorPage.jsx", "export default function DoctorPage(){ return <div>Doctor Dashboard</div>; }")
w(ROOT / "src/pages/PharmacyPage.jsx", "export default function PharmacyPage(){ return <div>Pharmacy Dashboard</div>; }")
w(ROOT / "src/pages/ManagerPage.jsx", "export default function ManagerPage(){ return <div>Manager Dashboard</div>; }")
w(ROOT / "src/pages/AdminPage.jsx", "export default function AdminPage(){ return <div>Admin Dashboard</div>; }")
w(ROOT / "src/utils/tokenHelpers.js", "export const getAuth = () => JSON.parse(localStorage.getItem('auth') || '{}'); export const clearAuth = () => localStorage.removeItem('auth');")
w(ROOT / "src/utils/date.js", "export const formatDate = (value) => new Date(value).toLocaleString();")
w(
    ROOT / "src/App.jsx",
    """
import { Routes, Route } from 'react-router-dom';
import ProtectedRoute from './routes/ProtectedRoute';
import RoleRoute from './routes/RoleRoute';
import LoginPage from './pages/LoginPage';
import PatientPage from './pages/PatientPage';
import DoctorPage from './pages/DoctorPage';
import PharmacyPage from './pages/PharmacyPage';
import ManagerPage from './pages/ManagerPage';
import AdminPage from './pages/AdminPage';
export default function App(){
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/patient" element={<ProtectedRoute><RoleRoute roles={['PATIENT']}><PatientPage /></RoleRoute></ProtectedRoute>} />
    <Route path="/doctor" element={<ProtectedRoute><RoleRoute roles={['DOCTOR']}><DoctorPage /></RoleRoute></ProtectedRoute>} />
    <Route path="/pharmacy" element={<ProtectedRoute><RoleRoute roles={['PHARMACIST','TECHNICIAN']}><PharmacyPage /></RoleRoute></ProtectedRoute>} />
    <Route path="/manager" element={<ProtectedRoute><RoleRoute roles={['MANAGER']}><ManagerPage /></RoleRoute></ProtectedRoute>} />
    <Route path="/admin" element={<ProtectedRoute><RoleRoute roles={['ADMIN']}><AdminPage /></RoleRoute></ProtectedRoute>} />
    <Route path="/unauthorized" element={<div>Unauthorized</div>} />
    <Route path="*" element={<LoginPage />} />
  </Routes>;
}
""",
)

print("Frontend expanded")
