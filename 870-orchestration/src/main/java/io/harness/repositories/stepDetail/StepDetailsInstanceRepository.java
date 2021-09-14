/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.stepDetail;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.stepDetail.StepDetailInstance;

import java.util.List;
import org.springframework.data.repository.PagingAndSortingRepository;

@OwnedBy(HarnessTeam.PIPELINE)
@HarnessRepo
public interface StepDetailsInstanceRepository extends PagingAndSortingRepository<StepDetailInstance, String> {
  List<StepDetailInstance> findByNodeExecutionId(String nodeExecutionId);
}
