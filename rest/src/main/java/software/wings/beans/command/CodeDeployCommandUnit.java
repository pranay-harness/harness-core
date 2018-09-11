package software.wings.beans.command;

import static java.lang.String.format;
import static software.wings.beans.command.CodeDeployCommandExecutionData.Builder.aCodeDeployCommandExecutionData;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import com.amazonaws.services.codedeploy.model.AutoRollbackConfiguration;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.eraro.ErrorCode;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.cloudprovider.aws.AwsCodeDeployService;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by anubhaw on 6/23/17.
 */
public class CodeDeployCommandUnit extends AbstractCommandUnit {
  private static final Logger logger = LoggerFactory.getLogger(CodeDeployCommandUnit.class);

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

    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(
        logService, context.getAccountId(), context.getAppId(), context.getActivityId(), getName());
    CommandExecutionStatus commandExecutionStatus;

    try {
      executionLogCallback.saveExecutionLog(
          format("Deploying application [%s] with following configuration.", applicationName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(format("Application Name: [%s]", applicationName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(format("Aws Region: [%s]", codeDeployParams.getRegion()), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(format("Deployment Group: [%s]", deploymentGroupName), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          format("Deployment Configuration: [%s]", Optional.of(deploymentConfigurationName).orElse("DEFAULT")),
          LogLevel.INFO);
      executionLogCallback.saveExecutionLog(format("Enable Auto Rollback: [%s]", enableAutoRollback), LogLevel.INFO);
      if (enableAutoRollback) {
        executionLogCallback.saveExecutionLog(
            format("Auto Rollback Configurations: [%s]", Joiner.on(",").join(autoRollbackConfigurations)),
            LogLevel.INFO);
      }
      executionLogCallback.saveExecutionLog(
          format("Ignore ApplicationStop lifecycle event failure: [%s]", ignoreApplicationStopFailures), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(format("Content options : [%s]", fileExistsBehavior), LogLevel.INFO);
      executionLogCallback.saveExecutionLog(
          format("Revision: [Type: %s, Bucket: %s, Bundle: %s, Key: %s]", revision.getRevisionType(),
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

      CodeDeployDeploymentInfo codeDeployDeploymentInfo =
          awsCodeDeployService.deployApplication(codeDeployParams.getRegion(), cloudProviderSetting,
              context.getCloudProviderCredentials(), createDeploymentRequest, executionLogCallback);
      commandExecutionStatus = codeDeployDeploymentInfo.getStatus();
      // go over instance data in command execution data and prepare execution data
      context.setCommandExecutionData(aCodeDeployCommandExecutionData()
                                          .withInstances(codeDeployDeploymentInfo.getInstances())
                                          .withDeploymentId(codeDeployDeploymentInfo.getDeploymentId())
                                          .build());
    } catch (Exception ex) {
      Misc.logAllMessages(ex, executionLogCallback);
      if (ex instanceof WingsException) {
        throw ex;
      }
      throw new WingsException(ErrorCode.GENERAL_ERROR, ex).addParam("message", Misc.getMessage(ex));
    }
    executionLogCallback.saveExecutionLog(
        format("Deployment finished with status [%s]", commandExecutionStatus), LogLevel.INFO);
    return commandExecutionStatus;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @JsonTypeName("CODE_DEPLOY")
  public static class Yaml extends AbstractCommandUnit.Yaml {
    public Yaml() {
      super(CommandUnitType.CODE_DEPLOY.name());
    }

    @Builder
    public Yaml(String name, String deploymentType) {
      super(name, CommandUnitType.CODE_DEPLOY.name(), deploymentType);
    }
  }
}
