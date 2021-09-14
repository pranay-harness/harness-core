/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.event.QueryRecordEntity;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.PIPELINE)
public interface QueryRecordsRepository
    extends PagingAndSortingRepository<QueryRecordEntity, String>, QueryRecordsRepositoryCustom {
  <S extends QueryRecordEntity> S save(S QueryRecordEntity);
}
