import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchPharmacyPrescriptions, updatePharmacyPrescriptionStatus } from '../features/pharmacySlice';
import ErrorBanner from '../components/ErrorBanner';
import LoadingSpinner from '../components/LoadingSpinner';

const statuses = ['PROCESSING', 'READY_FOR_PICKUP', 'REJECTED'];

export default function PharmacyPage() {
  const dispatch = useDispatch();
  const { items, loading, error } = useSelector((s) => s.pharmacy);
  const [statusById, setStatusById] = useState({});
  const [reasonById, setReasonById] = useState({});

  useEffect(() => {
    dispatch(fetchPharmacyPrescriptions());
  }, [dispatch]);

  const submit = async (id) => {
    const status = statusById[id] || 'PROCESSING';
    const rejectionReason = reasonById[id] || null;
    if (status === 'REJECTED' && !rejectionReason) {
      alert('Rejection reason is required for REJECTED status');
      return;
    }
    const result = await dispatch(updatePharmacyPrescriptionStatus({ id, status, rejectionReason }));
    if (updatePharmacyPrescriptionStatus.fulfilled.match(result)) {
      dispatch(fetchPharmacyPrescriptions());
    }
  };

  return (
    <div className="card">
      <h2>Pharmacy Dashboard</h2>
      <button type="button" onClick={() => dispatch(fetchPharmacyPrescriptions())} disabled={loading}>
        Refresh
      </button>
      {loading && <LoadingSpinner />}
      <ErrorBanner message={error} />
      <table width="100%" cellPadding="8" style={{ borderCollapse: 'collapse', marginTop: '1rem' }}>
        <thead>
          <tr>
            <th align="left">Prescription ID</th>
            <th align="left">Patient</th>
            <th align="left">Medication</th>
            <th align="left">Current Status</th>
            <th align="left">Update</th>
          </tr>
        </thead>
        <tbody>
          {items.map((p) => {
            const id = String(p.prescriptionId);
            const selected = statusById[id] || 'PROCESSING';
            return (
              <tr key={id}>
                <td>{id}</td>
                <td>{p.patientName}</td>
                <td>{p.medicationName}</td>
                <td>{p.status}</td>
                <td>
                  <select value={selected} onChange={(e) => setStatusById((prev) => ({ ...prev, [id]: e.target.value }))}>
                    {statuses.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                  {selected === 'REJECTED' && (
                    <input
                      placeholder="Rejection reason"
                      value={reasonById[id] || ''}
                      onChange={(e) => setReasonById((prev) => ({ ...prev, [id]: e.target.value }))}
                      style={{ marginLeft: '0.5rem' }}
                    />
                  )}
                  <button type="button" onClick={() => submit(id)} style={{ marginLeft: '0.5rem' }}>
                    Update
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
