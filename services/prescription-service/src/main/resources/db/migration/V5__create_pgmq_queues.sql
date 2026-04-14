DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pgmq.meta WHERE queue_name = 'prescription_created') THEN
        PERFORM pgmq.create('prescription_created');
    END IF;
END
$$;
