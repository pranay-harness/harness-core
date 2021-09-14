/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.aws.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.service.impl.aws.model.AwsCodeDeployRequest.AwsCodeDeployRequestType.LIST_DEPLOYMENT_INSTANCES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class AwsCodeDeployListDeploymentInstancesRequest extends AwsCodeDeployRequest {
  private String deploymentId;

  @Builder
  public AwsCodeDeployListDeploymentInstancesRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String deploymentId) {
    super(awsConfig, encryptionDetails, LIST_DEPLOYMENT_INSTANCES, region);
    this.deploymentId = deploymentId;
  }
}
