-- Copyright 2021 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Shield 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

CREATE OR REPLACE FUNCTION update_updatedAt_column()
RETURNS TRIGGER AS ' declare BEGIN NEW.updatedat = (NOW()); RETURN NEW; END;'
language 'plpgsql';
