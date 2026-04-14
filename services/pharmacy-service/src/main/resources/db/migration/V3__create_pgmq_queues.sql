DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pgmq.meta WHERE queue_name = 'notifications') THEN
        PERFORM pgmq.create('notifications');
    END IF;
END
$$;
