package software.wings.helpers.ext.cloudformation.request;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CloudFormationCreateStackRequest extends CloudFormationCommandRequest {
  public static final String CLOUD_FORMATION_STACK_CREATE_URL = "Create URL";
  public static final String CLOUD_FORMATION_STACK_CREATE_BODY = "Create Body";
  public static final String CLOUD_FORMATION_STACK_CREATE_GIT = "Create GIT";
  private String createType;
  private String data;
  private String stackNameSuffix;
  private String customStackName;
  private Map<String, String> variables;
  private Map<String, EncryptedDataDetail> encryptedVariables;
  private GitFileConfig gitFileConfig;
  private List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private GitConfig gitConfig;

  @Builder
  public CloudFormationCreateStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, AwsConfig awsConfig, int timeoutInMs, String createType, String data,
      String stackNameSuffix, String cloudFormationRoleArn, Map<String, String> variables, String region,
      String customStackName, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, Map<String, EncryptedDataDetail> encryptedVariables) {
    super(
        commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region, cloudFormationRoleArn);
    this.createType = createType;
    this.data = data;
    this.stackNameSuffix = stackNameSuffix;
    this.variables = variables;
    this.customStackName = customStackName;
    this.gitFileConfig = gitFileConfig;
    this.gitConfig = gitConfig;
    this.sourceRepoEncryptionDetails = encryptedDataDetails;
    this.encryptedVariables = encryptedVariables;
  }
}
