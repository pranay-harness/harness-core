/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
