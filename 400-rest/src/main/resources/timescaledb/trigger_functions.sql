CREATE OR REPLACE FUNCTION update_updatedAt_column()
RETURNS TRIGGER AS ' declare BEGIN NEW.updatedat = (NOW()); RETURN NEW; END;'
language 'plpgsql';