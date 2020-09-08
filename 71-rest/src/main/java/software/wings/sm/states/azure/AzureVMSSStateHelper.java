package software.wings.sm.states.azure;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.azure.model.AzureConstants.DELETE_NEW_VMSS;
import static io.harness.azure.model.AzureConstants.DEPLOYMENT_STATUS;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.SETUP_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.STEADY_STATE_TIMEOUT_REGEX;
import static io.harness.azure.model.AzureConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.azure.model.AzureConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.beans.OrchestrationWorkflowType.BLUE_GREEN;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.data.encoding.EncodingUtils;
import io.harness.delegate.beans.azure.AzureConfigDTO;
import io.harness.delegate.beans.azure.AzureConfigDelegate;
import io.harness.delegate.beans.azure.AzureVMAuthDTO;
import io.harness.delegate.beans.azure.AzureVMAuthDelegate;
import io.harness.delegate.task.azure.response.AzureVMInstanceData;
import io.harness.delegate.task.azure.response.AzureVMSSTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.Misc;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.InstanceElement;
import software.wings.api.PhaseElement;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Log;
import software.wings.beans.ResizeStrategy;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.AzureVMSSDummyCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.container.UserDataSpecification;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.InstanceStatusSummary;
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
  @Inject private AzureStateHelper azureStateHelper;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private SettingsService settingsService;
  @Inject private SweepingOutputService sweepingOutputService;
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
        aLog().appId(activity.getAppId()).activityId(activity.getUuid()).commandUnitName(commandUnitName);
    return new ManagerExecutionLogCallback(logService, logBuilder, activity.getUuid());
  }

  public Service getServiceByAppId(ExecutionContext context, String appId) {
    String serviceId = getServiceId(context);
    return serviceResourceService.getWithDetails(appId, serviceId);
  }

  private String getServiceId(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    return phaseElement.getServiceElement().getUuid();
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

  public Activity createAndSaveActivity(ExecutionContext context, Artifact artifact, String commandName,
      String commandType, CommandUnitDetails.CommandUnitType commandUnitType, List<CommandUnit> commandUnits) {
    WorkflowStandardParams workflowStandardParams = getWorkflowStandardParams(context);
    Application app = getApplication(context);
    Environment env = getEnvironment(context);
    Service service = getServiceByAppId(context, app.getUuid());

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .appId(app.getAppId())
                                          .environmentId(env.getUuid())
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType())
                                          .serviceId(service.getUuid())
                                          .serviceName(service.getName())
                                          .commandName(commandName)
                                          .commandType(commandType)
                                          .type(Activity.Type.Command)
                                          .workflowExecutionId(context.getWorkflowExecutionId())
                                          .workflowId(context.getWorkflowId())
                                          .workflowType(context.getWorkflowType())
                                          .workflowExecutionName(context.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                                          .commandUnitType(commandUnitType)
                                          .commandUnits(commandUnits)
                                          .status(ExecutionStatus.RUNNING)
                                          .triggeredBy(TriggeredBy.builder()
                                                           .email(workflowStandardParams.getCurrentUser().getEmail())
                                                           .name(workflowStandardParams.getCurrentUser().getName())
                                                           .build());

    if (artifact != null) {
      activityBuilder.artifactName(artifact.getDisplayName()).artifactId(artifact.getUuid());
      activityBuilder.artifactStreamName(artifact.getDisplayName()).artifactStreamId(artifact.getUuid());
    }

    return activityService.save(activityBuilder.build());
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

  public int renderTimeoutExpressionOrGetDefault(String timeout, ExecutionContext context, int defaultValue) {
    timeout = timeout.replaceAll(STEADY_STATE_TIMEOUT_REGEX, EMPTY);
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

  public List<EncryptedDataDetail> getEncryptedDataDetails(ExecutionContext context, final String settingId) {
    SettingAttribute settingAttribute = settingsService.get(settingId);
    return secretManager.getEncryptionDetails(
        (EncryptableSetting) settingAttribute.getValue(), context.getAppId(), context.getWorkflowExecutionId());
  }

  public List<EncryptedDataDetail> getServiceVariableEncryptedDataDetails(
      ExecutionContext context, ServiceVariable serviceVariable) {
    return secretManager.getEncryptionDetails(serviceVariable, context.getAppId(), context.getWorkflowExecutionId());
  }

  public AzureConfig getAzureConfig(final String computeProviderSettingId) {
    SettingAttribute settingAttribute = settingsService.get(computeProviderSettingId);
    return (AzureConfig) settingAttribute.getValue();
  }

  public HostConnectionAttributes getHostConnectionAttributes(final String hostConnectionAttrsKeyRefId) {
    SettingAttribute settingAttribute = settingsService.get(hostConnectionAttrsKeyRefId);
    return (HostConnectionAttributes) settingAttribute.getValue();
  }

  public ServiceVariable buildEncryptedServiceVariable(
      final String accountId, final String appId, final String envId, final String secretTextName) {
    EncryptedData encryptedData = secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretTextName);
    if (encryptedData == null) {
      throw new InvalidRequestException("No secret found with name + [" + secretTextName + "]", USER);
    }
    return ServiceVariable.builder()
        .accountId(accountId)
        .type(ENCRYPTED_TEXT)
        .encryptedValue(encryptedData.getUuid())
        .secretTextName(secretTextName)
        .build();
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

  public void setNewInstance(List<InstanceElement> newInstanceElements, boolean newInstance) {
    newInstanceElements.forEach(instanceElement -> instanceElement.setNewInstance(newInstance));
  }

  @NotNull
  public List<InstanceStatusSummary> getInstanceStatusSummaries(
      ExecutionStatus executionStatus, List<InstanceElement> newInstanceElements) {
    return newInstanceElements.stream()
        .map(instanceElement
            -> anInstanceStatusSummary().withInstanceElement(instanceElement).withStatus(executionStatus).build())
        .collect(toList());
  }

  public List<InstanceElement> generateInstanceElements(ExecutionContext context,
      AzureVMSSInfrastructureMapping azureVMSSInfrastructureMapping, List<AzureVMInstanceData> vmInstances) {
    return azureStateHelper.generateInstanceElements(context, azureVMSSInfrastructureMapping, vmInstances);
  }

  public void saveInstanceInfoToSweepingOutput(ExecutionContext context, List<InstanceElement> instanceElements) {
    if (isNotEmpty(instanceElements)) {
      // This sweeping element will be used by verification or other consumers.
      sweepingOutputService.save(
          context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
              .name(context.appendStateExecutionId(InstanceInfoVariables.SWEEPING_OUTPUT_NAME))
              .value(InstanceInfoVariables.builder()
                         .instanceElements(instanceElements)
                         .instanceDetails(azureStateHelper.generateAzureVMSSInstanceDetails(instanceElements))
                         .build())
              .build());
    }
  }

  public ExecutionStatus getExecutionStatus(AzureVMSSTaskExecutionResponse executionResponse) {
    return executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS ? ExecutionStatus.SUCCESS
                                                                                           : ExecutionStatus.FAILED;
  }

  public List<CommandUnit> generateDeployCommandUnits(
      ExecutionContext context, ResizeStrategy resizeStrategy, boolean isRollback) {
    List<CommandUnit> commandUnitList = null;
    if (OrchestrationWorkflowType.BLUE_GREEN == context.getOrchestrationWorkflowType()) {
      commandUnitList = ImmutableList.of(new AzureVMSSDummyCommandUnit(UP_SCALE_COMMAND_UNIT),
          new AzureVMSSDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT),
          new AzureVMSSDummyCommandUnit(DEPLOYMENT_STATUS));
    } else {
      commandUnitList = newArrayList();
      if (isRollback || ResizeStrategy.RESIZE_NEW_FIRST == resizeStrategy) {
        commandUnitList.add(new AzureVMSSDummyCommandUnit(UP_SCALE_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
      } else {
        commandUnitList.add(new AzureVMSSDummyCommandUnit(DOWN_SCALE_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(UP_SCALE_COMMAND_UNIT));
        commandUnitList.add(new AzureVMSSDummyCommandUnit(UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT));
      }
      if (isRollback) {
        commandUnitList.add(new AzureVMSSDummyCommandUnit(DELETE_NEW_VMSS));
      }
      commandUnitList.add(new AzureVMSSDummyCommandUnit(DEPLOYMENT_STATUS));
    }
    return commandUnitList;
  }

  public List<CommandUnit> generateSetupCommandUnits() {
    return ImmutableList.of(
        new AzureVMSSDummyCommandUnit(SETUP_COMMAND_UNIT), new AzureVMSSDummyCommandUnit(DEPLOYMENT_STATUS));
  }

  public AzureVMSSStateData populateStateData(ExecutionContext context) {
    Application application = getApplication(context);
    Service service = getServiceByAppId(context, application.getUuid());
    String serviceId = getServiceId(context);
    Artifact artifact = getArtifact((DeploymentExecutionContext) context, service.getUuid());
    AzureVMSSInfrastructureMapping azureVMSSInfrastructureMapping =
        getAzureVMSSInfrastructureMapping(context.fetchInfraMappingId(), application.getUuid());
    AzureConfig azureConfig = getAzureConfig(azureVMSSInfrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptedDataDetails =
        getEncryptedDataDetails(context, azureVMSSInfrastructureMapping.getComputeProviderSettingId());

    return AzureVMSSStateData.builder()
        .application(application)
        .service(service)
        .serviceId(serviceId)
        .artifact(artifact)
        .infrastructureMapping(azureVMSSInfrastructureMapping)
        .azureConfig(azureConfig)
        .azureEncryptedDataDetails(encryptedDataDetails)
        .build();
  }

  public AzureConfigDelegate createDelegateConfig(
      AzureConfig azureConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    AzureConfigDTO azureConfigDTO = AzureConfigDTO.builder()
                                        .clientId(azureConfig.getClientId())
                                        .encryptedKey(azureConfig.getEncryptedKey())
                                        .tenantId(azureConfig.getTenantId())
                                        .build();

    return AzureConfigDelegate.builder()
        .azureConfigDTO(azureConfigDTO)
        .azureEncryptionDetails(encryptedDataDetails)
        .build();
  }

  public AzureVMAuthDelegate createHostConnectionDelegate(
      HostConnectionAttributes connectionAttributes, List<EncryptedDataDetail> encryptedDataDetails) {
    AzureVMAuthDTO azureVMAuthDTO = AzureVMAuthDTO.builder()
                                        .authType(AzureVMAuthDTO.AuthType.SSH)
                                        .encryptedKey(connectionAttributes.getEncryptedKey())
                                        .build();
    return AzureVMAuthDelegate.builder()
        .azureVMAuthDTO(azureVMAuthDTO)
        .azureEncryptionDetails(encryptedDataDetails)
        .build();
  }

  public AzureVMAuthDelegate createVMAuthDelegate(
      ServiceVariable serviceVariable, List<EncryptedDataDetail> encryptedDataDetails) {
    AzureVMAuthDTO azureVMAuthDTO = AzureVMAuthDTO.builder()
                                        .authType(AzureVMAuthDTO.AuthType.PASSWORD)
                                        .encryptedKey(serviceVariable.getSecretTextName())
                                        .build();
    return AzureVMAuthDelegate.builder()
        .azureVMAuthDTO(azureVMAuthDTO)
        .azureEncryptionDetails(encryptedDataDetails)
        .build();
  }
}
