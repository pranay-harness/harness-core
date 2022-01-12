/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
