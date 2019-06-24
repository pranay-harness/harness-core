package software.wings.helpers.ext.cloudformation.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

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
  private GitFileConfig gitFileConfig;
  private List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private GitConfig gitConfig;

  @Builder
  public CloudFormationCreateStackRequest(CloudFormationCommandType commandType, String accountId, String appId,
      String activityId, String commandName, AwsConfig awsConfig, int timeoutInMs, String createType, String data,
      String stackNameSuffix, Map<String, String> variables, String region, String customStackName,
      GitFileConfig gitFileConfig, GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    super(commandType, accountId, appId, activityId, commandName, awsConfig, timeoutInMs, region);
    this.createType = createType;
    this.data = data;
    this.stackNameSuffix = stackNameSuffix;
    this.variables = variables;
    this.customStackName = customStackName;
    this.gitFileConfig = gitFileConfig;
    this.gitConfig = gitConfig;
    this.sourceRepoEncryptionDetails = encryptedDataDetails;
  }
}