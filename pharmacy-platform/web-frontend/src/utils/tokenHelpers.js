export const getAuth = () => JSON.parse(localStorage.getItem('auth') || '{}'); export const clearAuth = () => localStorage.removeItem('auth');
