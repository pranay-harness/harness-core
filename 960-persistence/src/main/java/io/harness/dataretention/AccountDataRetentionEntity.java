/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dataretention;

import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAccess;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.persistence.ValidUntilAccess;

public interface AccountDataRetentionEntity
    extends PersistentEntity, UuidAccess, AccountAccess, CreatedAtAccess, ValidUntilAccess {}
