import { useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { login, register } from '../features/authSlice';
import ErrorBanner from '../components/ErrorBanner';
import LoadingSpinner from '../components/LoadingSpinner';

const loginSchema = yup.object({
  email: yup.string().email().required(),
  password: yup.string().min(8).required()
});

const registerSchema = yup.object({
  firstName: yup.string().required(),
  lastName: yup.string().required(),
  email: yup.string().email().required().test(
    'role-domain',
    function (value) {
      const { role } = this.parent;
      if (!role || role === 'PATIENT') return true;
      const expected = `@${role.toLowerCase()}.com`;
      if (value && value.endsWith(expected)) return true;
      return this.createError({ message: `Email must end with ${expected} for the ${role} role` });
    }
  ),
  password: yup.string().min(8).required(),
  confirmPassword: yup.string().oneOf([yup.ref('password')], 'Passwords must match').required(),
  role: yup.string().oneOf(['PATIENT', 'DOCTOR', 'PHARMACIST', 'TECHNICIAN', 'MANAGER', 'ADMIN']).required()
});

const roleRoute = (role) => {
  if (role === 'DOCTOR') return '/doctor';
  if (role === 'PHARMACIST' || role === 'TECHNICIAN') return '/pharmacy';
  if (role === 'MANAGER') return '/manager';
  if (role === 'ADMIN') return '/admin';
  return '/patient';
};

export default function LoginPage() {
  const [mode, setMode] = useState('login');
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const auth = useSelector((s) => s.auth);

  const schema = useMemo(() => (mode === 'login' ? loginSchema : registerSchema), [mode]);
  const { register: rf, handleSubmit, formState: { errors }, reset } = useForm({ resolver: yupResolver(schema) });

  const onSubmit = async (values) => {
    if (mode === 'login') {
      const result = await dispatch(login(values));
      if (login.fulfilled.match(result)) {
        navigate(roleRoute(result.payload.user?.role));
      }
      return;
    }

    const { confirmPassword, ...payload } = values;
    const created = await dispatch(register(payload));
    if (register.fulfilled.match(created)) {
      setMode('login');
      reset({ email: payload.email, password: '' });
    }
  };

  return (
    <div className="card" style={{ maxWidth: 480, margin: '2rem auto' }}>
      <h2>{mode === 'login' ? 'Login' : 'Register'}</h2>
      <form onSubmit={handleSubmit(onSubmit)} style={{ display: 'grid', gap: '0.5rem' }}>
        {mode === 'register' && (
          <>
            <input placeholder="First name" {...rf('firstName')} />
            <small>{errors.firstName?.message}</small>
            <input placeholder="Last name" {...rf('lastName')} />
            <small>{errors.lastName?.message}</small>
          </>
        )}
        <input placeholder="Email" {...rf('email')} />
        <small>{errors.email?.message}</small>
        <input type="password" placeholder="Password" {...rf('password')} />
        <small>{errors.password?.message}</small>
        {mode === 'register' && (
          <>
            <input type="password" placeholder="Confirm password" {...rf('confirmPassword')} />
            <small>{errors.confirmPassword?.message}</small>
            <select {...rf('role')}>
              <option value="">Select role</option>
              <option value="PATIENT">PATIENT</option>
              <option value="DOCTOR">DOCTOR</option>
              <option value="PHARMACIST">PHARMACIST</option>
              <option value="TECHNICIAN">TECHNICIAN</option>
              <option value="MANAGER">MANAGER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <small>{errors.role?.message}</small>
          </>
        )}
        <button type="submit" disabled={auth.loading}>{mode === 'login' ? 'Login' : 'Create account'}</button>
      </form>
      {auth.loading && <LoadingSpinner />}
      <ErrorBanner message={auth.error} />
      <button
        type="button"
        onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); reset({}); }}
        style={{ marginTop: '1rem' }}
      >
        {mode === 'login' ? 'Need an account? Register' : 'Already have an account? Login'}
      </button>
    </div>
  );
}
