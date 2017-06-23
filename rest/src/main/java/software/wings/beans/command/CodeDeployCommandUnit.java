package software.wings.beans.command;

import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.inject.Inject;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext.CodoDeployParams;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;

/**
 * Created by anubhaw on 6/23/17.
 */
public class CodeDeployCommandUnit extends AbstractCommandUnit {
  @Inject @Transient private transient AwsCodeDeployService awsCodeDeployService;

  @Inject @Transient private transient DelegateLogService logService;

  public CodeDeployCommandUnit() {
    super(CommandUnitType.CODE_DEPLOY);
    setArtifactNeeded(true);
    setDeploymentType(DeploymentType.CODEDEPLOY.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    CodoDeployParams codoDeployParams = context.getCodoDeployParams();
    String region = codoDeployParams.getRegion();
    String deploymentGroupName = codoDeployParams.getDeploymentGroupName();
    String applicationName = codoDeployParams.getApplicationName();
    String deploymentConfigurationName = codoDeployParams.getDeploymentConfigurationName();

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      CreateDeploymentRequest createDeploymentRequest = new CreateDeploymentRequest()
                                                            .withApplicationName(applicationName)
                                                            .withDeploymentGroupName(deploymentGroupName)
                                                            .withDeploymentConfigName(deploymentConfigurationName);
      CodeDeployDeploymentInfo codeDeployDeploymentInfo = awsCodeDeployService.deployApplication(
          region, cloudProviderSetting, createDeploymentRequest, executionLogCallback);
      commandExecutionStatus = codeDeployDeploymentInfo.getStatus();
      // go over instance data in command execution data and prepare execution data
      context.setCommandExecutionData(new CodeDeployCommndExecutionData());
    } catch (Exception ex) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    return commandExecutionStatus;
  }
}
