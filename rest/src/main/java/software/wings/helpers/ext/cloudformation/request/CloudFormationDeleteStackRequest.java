package software.wings.helpers.ext.cloudformation.request;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationDeleteStackRequest extends CloudFormationCommandRequest {
  private String stackId;

  @Builder
  public CloudFormationDeleteStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, AwsConfig awsConfig, int timeoutInMs, String stackId) {
    super(commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs);
    this.stackId = stackId;
  }
}