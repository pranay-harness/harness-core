/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.analysis;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic Data Collection info for All Verification providers
 * Created by Pranjal on 03/31/2019
 */
@Data
@AllArgsConstructor
public class DataCollectionInfo {
  private String accountId;
  private String applicationId;
  private String stateExecutionId;
  private String cvConfigId;
  private String workflowId;
  private String workflowExecutionId;
  private String serviceId;

  public void copy(DataCollectionInfo clone) {
    clone.setAccountId(accountId);
    clone.setApplicationId(applicationId);
    clone.setStateExecutionId(stateExecutionId);
    clone.setCvConfigId(cvConfigId);
    clone.setWorkflowId(workflowId);
    clone.setWorkflowExecutionId(workflowExecutionId);
    clone.setServiceId(serviceId);
  }
}
