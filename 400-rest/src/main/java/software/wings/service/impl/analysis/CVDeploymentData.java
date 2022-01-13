/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.analysis;

import io.harness.beans.ExecutionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Created by Praveen
 */
@Data
@Builder
@AllArgsConstructor
public class CVDeploymentData {
  String appId;
  String envId;
  String serviceId;
  String accountId;
  ExecutionStatus status;
  long startTs;
  String workflowExecutionId;
  String pipelineExecutionId;
  String workflowName;
  String pipelineName;

  public CVDeploymentData(ContinuousVerificationExecutionMetaData cvMetadata) {
    this.accountId = cvMetadata.getAccountId();
    this.appId = cvMetadata.getApplicationId();
    this.workflowExecutionId = cvMetadata.getWorkflowExecutionId();
    this.pipelineExecutionId = cvMetadata.getPipelineExecutionId();
    this.serviceId = cvMetadata.getServiceId();
    this.envId = cvMetadata.getEnvId();
    this.startTs = cvMetadata.getWorkflowStartTs();
  }
}
