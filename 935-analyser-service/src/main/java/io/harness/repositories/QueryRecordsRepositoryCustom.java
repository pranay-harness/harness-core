/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface QueryRecordsRepositoryCustom {
  List<QueryRecordEntity> findAllHashes(int page, int size);
}
