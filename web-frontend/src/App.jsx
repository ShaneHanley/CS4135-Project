import { Routes, Route } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useEffect } from 'react';
import ProtectedRoute from './routes/ProtectedRoute';
import RoleRoute from './routes/RoleRoute';
import { fetchMe, logout } from './features/authSlice';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import LoginPage from './pages/LoginPage';
import PatientPage from './pages/PatientPage';
import DoctorPage from './pages/DoctorPage';
import PharmacyPage from './pages/PharmacyPage';
import ManagerPage from './pages/ManagerPage';
import AdminPage from './pages/AdminPage';
export default function App(){
  const dispatch = useDispatch();
  const auth = useSelector((s) => s.auth);
  useEffect(() => {
    if (auth.accessToken && !auth.user) {
      dispatch(fetchMe());
    }
  }, [auth.accessToken, auth.user, dispatch]);

  return (
    <div className="app-shell">
      <Navbar />
      <main className="container">
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/patient" element={<ProtectedRoute><RoleRoute roles={['PATIENT']}><PatientPage /></RoleRoute></ProtectedRoute>} />
          <Route path="/doctor" element={<ProtectedRoute><RoleRoute roles={['DOCTOR']}><DoctorPage /></RoleRoute></ProtectedRoute>} />
          <Route path="/pharmacy" element={<ProtectedRoute><RoleRoute roles={['PHARMACIST','TECHNICIAN','MANAGER']}><PharmacyPage /></RoleRoute></ProtectedRoute>} />
          <Route path="/manager" element={<ProtectedRoute><RoleRoute roles={['MANAGER']}><ManagerPage /></RoleRoute></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute><RoleRoute roles={['ADMIN']}><AdminPage /></RoleRoute></ProtectedRoute>} />
          <Route path="/logout" element={<button type="button" onClick={() => dispatch(logout())}>Logged out, click again if needed</button>} />
          <Route path="/unauthorized" element={<div>Unauthorized</div>} />
          <Route path="*" element={<LoginPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  );
}
