-- Copyright 2021 Harness Inc.
-- 
-- Licensed under the Apache License, Version 2.0
-- http://www.apache.org/licenses/LICENSE-2.0

CREATE OR REPLACE FUNCTION update_updatedAt_column()
RETURNS TRIGGER AS ' declare BEGIN NEW.updatedat = (NOW()); RETURN NEW; END;'
language 'plpgsql';
