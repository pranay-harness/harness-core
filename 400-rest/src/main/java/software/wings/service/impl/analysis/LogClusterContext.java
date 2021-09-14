/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import software.wings.sm.StateType;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by sriram_parthasarathy on 8/29/17.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogClusterContext {
  private String accountId;
  private String appId;
  private String workflowId;
  private String workflowExecutionId;
  private String stateExecutionId;
  private String serviceId;
  private Set<String> controlNodes;
  private Set<String> testNodes;
  private String query;
  private boolean isSSL;
  private int appPort;
  private StateType stateType;
  private String stateBaseUrl;
}
