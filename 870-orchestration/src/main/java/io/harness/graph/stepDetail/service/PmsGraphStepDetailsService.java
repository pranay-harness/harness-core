/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.graph.stepDetail.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.data.stepdetails.PmsStepDetails;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PmsGraphStepDetailsService {
  void addStepDetail(String nodeExecutionId, String planExecutionId, PmsStepDetails stepDetails, String name);
  Map<String, PmsStepDetails> getStepDetails(String planExecutionId, String nodeExecutionId);
}
