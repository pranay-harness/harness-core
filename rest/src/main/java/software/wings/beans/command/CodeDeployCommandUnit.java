package software.wings.beans.command;

import static software.wings.beans.command.CodeDeployCommandExecutionData.Builder.aCodeDeployCommandExecutionData;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.inject.Inject;

import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext.CodeDeployParams;
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
    setDeploymentType(DeploymentType.AWS_CODEDEPLOY.name());
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    SettingAttribute cloudProviderSetting = context.getCloudProviderSetting();
    CodeDeployParams codeDeployParams = context.getCodeDeployParams();
    String region = context.getRegion();
    String deploymentGroupName = codeDeployParams.getDeploymentGroupName();
    String applicationName = codeDeployParams.getApplicationName();
    String deploymentConfigurationName = codeDeployParams.getDeploymentConfigurationName();
    RevisionLocation revision = new RevisionLocation().withRevisionType("S3").withS3Location(
        new S3Location()
            .withBucket(codeDeployParams.getBucket())
            .withBundleType(codeDeployParams.getBundleType())
            .withKey(codeDeployParams.getKey()));

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(context, getName());
    executionLogCallback.setLogService(logService);
    CommandExecutionStatus commandExecutionStatus = FAILURE;

    try {
      executionLogCallback.saveExecutionLog(
          String.format("Deploying application [%s] with following configuration.", applicationName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(String.format("Application Name: [%s]", applicationName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(String.format("Aws Region: [%s]", region), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          String.format("Deployment Group: [%s]", deploymentGroupName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          String.format("Deployment Configuration: [%s]",
              deploymentConfigurationName == null ? "DEFAULT" : deploymentConfigurationName),
          LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          String.format("Revision: [Type: %s, Bucket: %s, Bundle: %s, Key: %s]", revision.getRevisionType(),
              revision.getS3Location().getBucket(), revision.getS3Location().getBundleType(),
              revision.getS3Location().getKey()),
          LogLevel.INFO);

      /*
        create-deployment
                                    [--application-name <value>]
                                    [--deployment-group-name <value>]
                                    [--revision <value>]
                                    [--deployment-config-name <value>]
                                    [--description <value>]**
[--ignore-application-stop-failures | --no-ignore-application-stop-failures]
                                    [--target-instances <value>]**
[--auto-rollback-configuration <value>]
[--update-outdated-instances-only | --no-update-outdated-instances-only]
[--file-exists-behavior <value>]
                                    [--s3-location <value>]**
[--github-location <value>]

       */

      CreateDeploymentRequest createDeploymentRequest = new CreateDeploymentRequest()
                                                            .withApplicationName(applicationName)
                                                            .withDeploymentGroupName(deploymentGroupName)
                                                            .withDeploymentConfigName(deploymentConfigurationName)
                                                            .withRevision(revision);
      CodeDeployDeploymentInfo codeDeployDeploymentInfo = awsCodeDeployService.deployApplication(
          region, cloudProviderSetting, createDeploymentRequest, executionLogCallback);
      commandExecutionStatus = codeDeployDeploymentInfo.getStatus();
      // go over instance data in command execution data and prepare execution data
      context.setCommandExecutionData(
          aCodeDeployCommandExecutionData().withInstances(codeDeployDeploymentInfo.getInstances()).build());
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "", ex);
    }
    executionLogCallback.saveExecutionLog(
        String.format("Deployment finished with status [%s]", commandExecutionStatus), LogLevel.INFO);
    return commandExecutionStatus;
  }
}
