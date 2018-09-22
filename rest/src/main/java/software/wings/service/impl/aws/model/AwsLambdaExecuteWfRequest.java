package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsLambdaRequest.AwsLambdaRequestType.EXECUTE_LAMBA_WF;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaExecuteWfRequest extends AwsLambdaRequest {
  private String accountId;
  private String appId;
  private String activityId;
  private String commandName;
  private List<AwsLambdaFunctionParams> functionParams;
  private String roleArn;
  private List<String> evaluatedAliases;
  private Map<String, String> serviceVariables;
  private AwsLambdaVpcConfig lambdaVpcConfig;

  @Builder
  public AwsLambdaExecuteWfRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      List<AwsLambdaFunctionParams> functionParams, String roleArn, List<String> evaluatedAliases,
      Map<String, String> serviceVariables, AwsLambdaVpcConfig lambdaVpcConfig, String accountId, String appId,
      String activityId, String commandName) {
    super(awsConfig, encryptionDetails, EXECUTE_LAMBA_WF, region);
    this.accountId = accountId;
    this.appId = appId;
    this.activityId = activityId;
    this.commandName = commandName;
    this.functionParams = functionParams;
    this.roleArn = roleArn;
    this.evaluatedAliases = evaluatedAliases;
    this.serviceVariables = serviceVariables;
    this.lambdaVpcConfig = lambdaVpcConfig;
  }
}