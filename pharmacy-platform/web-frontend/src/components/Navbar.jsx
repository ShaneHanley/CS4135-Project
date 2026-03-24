import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { logout } from '../features/authSlice';

const homePathForRole = (role) => {
  if (role === 'DOCTOR') return '/doctor';
  if (role === 'PHARMACIST' || role === 'TECHNICIAN') return '/pharmacy';
  if (role === 'MANAGER') return '/manager';
  if (role === 'ADMIN') return '/admin';
  return '/patient';
};

export default function Navbar() {
  const auth = useSelector((s) => s.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const role = auth.user?.role;

  return (
    <nav className="nav">
      <div><strong>Pharmacy Platform MVP</strong></div>
      <div className="nav-links">
        {role && <Link to={homePathForRole(role)}>Home</Link>}
        {auth.user?.email && <span>{auth.user.email}</span>}
        {role && <span>({role})</span>}
        {auth.accessToken && (
          <button
            type="button"
            onClick={() => {
              dispatch(logout());
              navigate('/login');
            }}
          >
            Logout
          </button>
        )}
      </div>
    </nav>
  );
}
