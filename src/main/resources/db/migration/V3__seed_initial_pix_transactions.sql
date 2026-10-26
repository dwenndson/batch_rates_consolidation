INSERT INTO transacao_pix (
    end_to_end_id,
    txid,
    valor,
    horario,
    pagador,
    devolucoes,
    status
) VALUES
    ('E2E-SEED-0001', 'TXID-SEED-0001', 10.00, CURRENT_TIMESTAMP, '{}'::jsonb, '[]'::jsonb, 'PENDING'),
    ('E2E-SEED-0002', 'TXID-SEED-0002', 25.50, CURRENT_TIMESTAMP, '{}'::jsonb, '[]'::jsonb, 'PENDING'),
    ('E2E-SEED-0003', 'TXID-SEED-0003', 100.00, CURRENT_TIMESTAMP, '{}'::jsonb, '[]'::jsonb, 'PENDING')
ON CONFLICT (end_to_end_id) DO NOTHING;
