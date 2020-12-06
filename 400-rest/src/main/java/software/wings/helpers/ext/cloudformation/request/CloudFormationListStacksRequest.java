package software.wings.helpers.ext.cloudformation.request;

import software.wings.beans.AwsConfig;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationListStacksRequest extends CloudFormationCommandRequest {
  private String stackId;

  @Builder
  public CloudFormationListStacksRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, String cloudFormationRoleArn, AwsConfig awsConfig, int timeoutInMs,
      String stackId, String region) {
    super(
        commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region, cloudFormationRoleArn);
    this.stackId = stackId;
  }
}
