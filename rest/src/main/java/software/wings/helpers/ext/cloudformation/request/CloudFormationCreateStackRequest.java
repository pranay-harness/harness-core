package software.wings.helpers.ext.cloudformation.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationCreateStackRequest extends CloudFormationCommandRequest {
  public static final String CLOUD_FORMATION_STACK_CREATE_URL = "Create URL";
  public static final String CLOUD_FORMATION_STACK_CREATE_BODY = "Create Body";
  private String createType;
  private String data;
  private String stackNameSuffix;
  private Map<String, String> variables;

  @Builder
  public CloudFormationCreateStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, AwsConfig awsConfig, int timeoutInMs, String createType, String data,
      String stackNameSuffix, Map<String, String> variables, String region) {
    super(commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region);
    this.createType = createType;
    this.data = data;
    this.stackNameSuffix = stackNameSuffix;
    this.variables = variables;
  }
}