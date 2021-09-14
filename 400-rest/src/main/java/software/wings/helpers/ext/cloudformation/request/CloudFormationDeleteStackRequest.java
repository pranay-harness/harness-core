/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.cloudformation.request;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.AwsConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class CloudFormationDeleteStackRequest extends CloudFormationCommandRequest {
  private String stackNameSuffix;
  private String customStackName;

  @Builder
  public CloudFormationDeleteStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, String cloudFormationRoleArn, AwsConfig awsConfig, int timeoutInMs,
      String stackNameSuffix, String region, String customStackName) {
    super(
        commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region, cloudFormationRoleArn);
    this.stackNameSuffix = stackNameSuffix;
    this.customStackName = customStackName;
  }
}
