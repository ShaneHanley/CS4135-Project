import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { createPrescription, fetchPrescriptions } from '../features/prescriptionSlice';
import ErrorBanner from '../components/ErrorBanner';
import LoadingSpinner from '../components/LoadingSpinner';

const schema = yup.object({
  patientId: yup.string().required(),
  patientEmail: yup.string().email().required(),
  patientName: yup.string().required(),
  pharmacyId: yup.string().required(),
  medicationName: yup.string().required(),
  dosage: yup.string().required(),
  instructions: yup.string().required(),
  quantity: yup.number().integer().positive().required(),
  refillsAllowed: yup.number().integer().min(0).required()
});

export default function DoctorPage() {
  const dispatch = useDispatch();
  const { items, loading, error } = useSelector((s) => s.prescription);
  const { register, handleSubmit, reset, formState: { errors } } = useForm({
    resolver: yupResolver(schema),
    defaultValues: { pharmacyId: 'default-pharmacy', refillsAllowed: 0, quantity: 1 }
  });

  useEffect(() => {
    dispatch(fetchPrescriptions());
  }, [dispatch]);

  const onSubmit = async (values) => {
    const result = await dispatch(createPrescription(values));
    if (createPrescription.fulfilled.match(result)) {
      reset({ ...values, instructions: '', quantity: 1 });
    }
  };

  return (
    <div className="card">
      <h2>Doctor Dashboard</h2>
      <form onSubmit={handleSubmit(onSubmit)} className="grid" style={{ maxWidth: 640 }}>
        <input placeholder="Patient ID" {...register('patientId')} />
        <small>{errors.patientId?.message}</small>
        <input placeholder="Patient Email" {...register('patientEmail')} />
        <small>{errors.patientEmail?.message}</small>
        <input placeholder="Patient Name" {...register('patientName')} />
        <small>{errors.patientName?.message}</small>
        <input placeholder="Pharmacy ID" {...register('pharmacyId')} />
        <small>{errors.pharmacyId?.message}</small>
        <input placeholder="Medication Name" {...register('medicationName')} />
        <small>{errors.medicationName?.message}</small>
        <input placeholder="Dosage (e.g. 500mg)" {...register('dosage')} />
        <small>{errors.dosage?.message}</small>
        <textarea placeholder="Instructions" {...register('instructions')} />
        <small>{errors.instructions?.message}</small>
        <input type="number" placeholder="Quantity" {...register('quantity')} />
        <small>{errors.quantity?.message}</small>
        <input type="number" placeholder="Refills Allowed" {...register('refillsAllowed')} />
        <small>{errors.refillsAllowed?.message}</small>
        <button type="submit" disabled={loading}>Create Prescription</button>
      </form>

      <h3 style={{ marginTop: '1rem' }}>Sent Prescriptions</h3>
      {loading && <LoadingSpinner />}
      <ErrorBanner message={error} />
      <table width="100%" cellPadding="8" style={{ borderCollapse: 'collapse' }}>
        <thead>
          <tr>
            <th align="left">ID</th>
            <th align="left">Patient</th>
            <th align="left">Medication</th>
            <th align="left">Status</th>
          </tr>
        </thead>
        <tbody>
          {items.map((p) => (
            <tr key={p.prescriptionId}>
              <td>{p.prescriptionId}</td>
              <td>{p.patientName}</td>
              <td>{p.medicationName}</td>
              <td>{p.status}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
