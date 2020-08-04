package software.wings.sm.states.azure;

import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;

import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.Misc;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Log;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.container.UserDataSpecification;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.utils.ServiceVersionConvention;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class AzureVMSSStateHelper {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ActivityService activityService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private LogService logService;

  public boolean isBlueGreenWorkflow(ExecutionContext context) {
    return BLUE_GREEN == context.getOrchestrationWorkflowType();
  }

  public Artifact getArtifact(DeploymentExecutionContext context, String serviceId) {
    return Optional.ofNullable(context.getDefaultArtifactForService(serviceId))
        .orElseThrow(
            () -> new InvalidRequestException(format("Unable to find artifact for service id: %s", serviceId)));
  }

  public ManagerExecutionLogCallback getExecutionLogCallback(Activity activity) {
    String commandUnitName = activity.getCommandUnits().get(0).getName();
    Log.Builder logBuilder =
        aLog().withAppId(activity.getAppId()).withActivityId(activity.getUuid()).withCommandUnitName(commandUnitName);
    return new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());
  }

  public Service getServiceByAppId(ExecutionContext context, String appId) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();
    return serviceResourceService.getWithDetails(appId, serviceId);
  }

  public Application getApplication(ExecutionContext context) {
    return Optional.ofNullable(getWorkflowStandardParams(context))
        .map(WorkflowStandardParams::getApp)
        .orElseThrow(
            ()
                -> new InvalidRequestException(
                    format("Application can't be null or empty, accountId: %s", context.getAccountId()), USER));
  }

  public Environment getEnvironment(ExecutionContext context) {
    return Optional.ofNullable(getWorkflowStandardParams(context))
        .map(WorkflowStandardParams::getEnv)
        .orElseThrow(()
                         -> new InvalidRequestException(
                             format("Env can't be null or empty, accountId: %s", context.getAccountId()), USER));
  }

  @NotNull
  public WorkflowStandardParams getWorkflowStandardParams(ExecutionContext context) {
    return Optional.ofNullable((WorkflowStandardParams) context.getContextElement(ContextElementType.STANDARD))
        .orElseThrow(
            ()
                -> new InvalidRequestException(
                    format("WorkflowStandardParams can't be null or empty, accountId: %s", context.getAccountId()),
                    USER));
  }

  public Activity buildActivity(
      ExecutionContext context, Application app, Environment env, Service service, String commandName) {
    WorkflowStandardParams workflowStandardParams = getWorkflowStandardParams(context);

    Command command = getCommand(app.getUuid(), service.getUuid(), env.getUuid(), commandName);
    List<CommandUnit> commandUnitList =
        getCommandUnitList(app.getUuid(), service.getUuid(), env.getUuid(), commandName);

    return Activity.builder()
        .applicationName(app.getName())
        .environmentId(env.getUuid())
        .environmentName(env.getName())
        .environmentType(env.getEnvironmentType())
        .serviceId(service.getUuid())
        .serviceName(service.getName())
        .commandName(command.getName())
        .commandType(command.getCommandUnitType().name())
        .type(Activity.Type.Command)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .workflowId(context.getWorkflowId())
        .workflowType(context.getWorkflowType())
        .workflowExecutionName(context.getWorkflowExecutionName())
        .stateExecutionInstanceId(context.getStateExecutionInstanceId())
        .stateExecutionInstanceName(context.getStateExecutionInstanceName())
        .commandUnits(commandUnitList)
        .status(ExecutionStatus.RUNNING)
        .triggeredBy(TriggeredBy.builder()
                         .email(workflowStandardParams.getCurrentUser().getEmail())
                         .name(workflowStandardParams.getCurrentUser().getName())
                         .build())
        .build();
  }

  public Command getCommand(String appId, String serviceId, String envId, String commandName) {
    return serviceResourceService.getCommandByName(appId, serviceId, envId, commandName).getCommand();
  }

  public List<CommandUnit> getCommandUnitList(String appId, String serviceId, String envId, String commandName) {
    return serviceResourceService.getFlattenCommandUnitList(appId, serviceId, envId, commandName);
  }

  public void updateActivityStatus(String appId, String activityId, ExecutionStatus executionStatus) {
    activityService.updateStatus(activityId, appId, executionStatus);
  }

  public Activity saveActivity(Activity activity, final String appId) {
    activity.setAppId(appId);
    return activityService.save(activity);
  }

  public int renderTimeoutExpressionOrGetDefault(String timeout, ExecutionContext context, int defaultValue) {
    int value = renderExpressionOrGetDefault(timeout, context, defaultValue);
    return value <= 0 ? defaultValue : value;
  }

  public int renderExpressionOrGetDefault(String expr, ExecutionContext context, int defaultValue) {
    int retVal = defaultValue;
    if (isNotEmpty(expr)) {
      try {
        retVal = Integer.parseInt(context.renderExpression(expr));
      } catch (NumberFormatException e) {
        logger.error(format("Number format Exception while evaluating: [%s]", expr), e);
        retVal = defaultValue;
      }
    }
    return retVal;
  }

  public String getBase64EncodedUserData(ExecutionContext context, String appId, String serviceId) {
    return Optional.ofNullable(serviceResourceService.getUserDataSpecification(appId, serviceId))
        .map(UserDataSpecification::getData)
        .map(context::renderExpression)
        .map(EncodingUtils::encodeBase64)
        .orElse(null);
  }

  public String fixNamePrefix(ExecutionContext context, final String name, final String appName,
      final String serviceName, final String envName) {
    return isBlank(name) ? Misc.normalizeExpression(ServiceVersionConvention.getPrefix(appName, serviceName, envName))
                         : Misc.normalizeExpression(context.renderExpression(name));
  }

  public AzureVMSSInfrastructureMapping getAzureVMSSInfrastructureMapping(String infraMappingId, String appId) {
    notNullCheck("Infrastructure Mapping id is null or empty", infraMappingId);
    notNullCheck("Application id is null or empty", appId);
    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(appId, infraMappingId);
    if (!(infrastructureMapping instanceof AzureVMSSInfrastructureMapping)) {
      throw new InvalidRequestException(
          format("Infrastructure Mapping is not instance of AzureVMSSInfrastructureMapping, infrastructureMapping: %s",
              infrastructureMapping));
    }
    return (AzureVMSSInfrastructureMapping) infrastructureMappingService.get(appId, infraMappingId);
  }

  public List<EncryptedDataDetail> getEncryptedDataDetails(
      ExecutionContext context, InfrastructureMapping infrastructureMapping) {
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    return secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
  }

  public AzureConfig getAzureConfig(InfrastructureMapping infrastructureMapping) {
    SettingAttribute settingAttribute = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    return (AzureConfig) settingAttribute.getValue();
  }

  public Integer getAzureVMSSStateTimeoutFromContext(ExecutionContext context) {
    AzureVMSSSetupContextElement azureVMSSSetupContextElement =
        context.getContextElement(ContextElementType.AZURE_VMSS_SETUP);
    return Optional.ofNullable(azureVMSSSetupContextElement)
        .map(AzureVMSSSetupContextElement::getAutoScalingSteadyStateVMSSTimeout)
        .filter(autoScalingSteadyStateVMSSTimeout -> autoScalingSteadyStateVMSSTimeout.intValue() > 0)
        .map(autoScalingSteadyStateVMSSTimeout
            -> Ints.checkedCast(TimeUnit.MINUTES.toMillis(autoScalingSteadyStateVMSSTimeout.longValue())))
        .orElse(null);
  }
}
