package software.wings.helpers.ext.cloudformation.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

@Data
@EqualsAndHashCode(callSuper = false)
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
