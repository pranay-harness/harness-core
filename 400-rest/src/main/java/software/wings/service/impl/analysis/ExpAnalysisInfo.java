/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.sm.StateType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExpAnalysisInfo {
  private String stateExecutionId;
  private String appId;
  private StateType stateType;
  private String expName;
  private String envId;
  private String workflowExecutionId;
  private long createdAt;
  private boolean mismatch;
}
