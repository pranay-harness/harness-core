package software.wings.beans.command;

import static software.wings.beans.command.CodeDeployCommandExecutionData.Builder.aCodeDeployCommandExecutionData;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import com.amazonaws.services.codedeploy.model.AutoRollbackConfiguration;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    boolean enableAutoRollback = codeDeployParams.isEnableAutoRollback();
    List<String> autoRollbackConfigurations = codeDeployParams.getAutoRollbackConfigurations() != null
        ? codeDeployParams.getAutoRollbackConfigurations()
        : new ArrayList<>();
    boolean ignoreApplicationStopFailures = codeDeployParams.isIgnoreApplicationStopFailures();

    String fileExistsBehavior = codeDeployParams.getFileExistsBehavior();

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
          String.format("Deployment Configuration: [%s]", Optional.of(deploymentConfigurationName).orElse("DEFAULT")),
          LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          String.format("Enable Auto Rollback: [%s]", enableAutoRollback), LogLevel.INFO);
      if (enableAutoRollback) {
        executionLogCallback.saveExecutionLog(
            String.format("Auto Rollback Configurations: [%s]", Joiner.on(",").join(autoRollbackConfigurations)),
            LogLevel.INFO);
      }
      executionLogCallback.saveExecutionLog(
          String.format("Ignore ApplicationStop lifecycle event failure: [%s]", ignoreApplicationStopFailures),
          LogLevel.INFO);
      executionLogCallback.saveExecutionLog(String.format("Content options : [%s]", fileExistsBehavior), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          String.format("Revision: [Type: %s, Bucket: %s, Bundle: %s, Key: %s]", revision.getRevisionType(),
              revision.getS3Location().getBucket(), revision.getS3Location().getBundleType(),
              revision.getS3Location().getKey()),
          LogLevel.INFO);

      CreateDeploymentRequest createDeploymentRequest =
          new CreateDeploymentRequest()
              .withApplicationName(applicationName)
              .withDeploymentGroupName(deploymentGroupName)
              .withDeploymentConfigName(deploymentConfigurationName)
              .withRevision(revision)
              .withIgnoreApplicationStopFailures(ignoreApplicationStopFailures)
              .withAutoRollbackConfiguration(new AutoRollbackConfiguration()
                                                 .withEnabled(enableAutoRollback)
                                                 .withEvents(autoRollbackConfigurations))
              .withFileExistsBehavior(fileExistsBehavior);

      CodeDeployDeploymentInfo codeDeployDeploymentInfo = awsCodeDeployService.deployApplication(region,
          cloudProviderSetting, context.getCloudProviderCredentials(), createDeploymentRequest, executionLogCallback);
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("CODE_DEPLOY")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml() {
      super();
      setCommandUnitType(CommandUnitType.CODE_DEPLOY.name());
    }

    public static final class Builder extends AbstractCommandUnit.Yaml.Builder {
      private Builder() {}

      public static Builder aYaml() {
        return new Builder();
      }

      @Override
      protected Yaml getCommandUnitYaml() {
        return new CodeDeployCommandUnit.Yaml();
      }
    }
  }
}
