/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.batch.processing.entities.AccountShardMapping;

import java.util.List;

public interface AccountShardMappingDao {
  List<AccountShardMapping> getAccountShardMapping();
}
