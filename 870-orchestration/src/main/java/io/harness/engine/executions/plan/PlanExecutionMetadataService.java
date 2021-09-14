/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.engine.executions.plan;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecutionMetadata;

import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PlanExecutionMetadataService {
  Optional<PlanExecutionMetadata> findByPlanExecutionId(String planExecutionId);
  PlanExecutionMetadata save(PlanExecutionMetadata planExecutionMetadata);

  String getYamlFromPlanExecutionId(String planExecutionId);
}
