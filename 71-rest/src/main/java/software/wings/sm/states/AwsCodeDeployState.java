package software.wings.sm.states;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.api.HostElement.Builder.aHostElement;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.api.InstanceElementListParam.InstanceElementListParamBuilder.anInstanceElementListParam;
import static software.wings.api.ServiceTemplateElement.Builder.aServiceTemplateElement;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.common.Constants.ARTIFACT_S3_BUCKET_EXPRESSION;
import static software.wings.common.Constants.ARTIFACT__S3_KEY_EXPRESSION;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.CommandStateExecutionData.Builder;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.InstanceElementListParam;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CodeDeployInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.command.CodeDeployCommandExecutionData;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.common.Constants;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.aws.model.AwsCodeDeployS3LocationData;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.aws.manager.AwsCodeDeployHelperServiceManager;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by brett on 6/22/17
 */
public class AwsCodeDeployState extends State {
  private static final Logger logger = LoggerFactory.getLogger(AwsCodeDeployState.class);

  @Attributes(title = "Bucket", required = true) private String bucket;
  @Attributes(title = "Key", required = true) private String key;
  @Attributes(title = "Steady State Timeout") @DefaultValue("10") private int steadyStateTimeout = 10;

  @EnumData(enumDataProvider = CodeDeployBundleTypeProvider.class)
  @Attributes(title = "Bundle Type", required = true)
  private String bundleType;

  @Attributes(title = "Ignore ApplicationStop lifecycle event failure") private boolean ignoreApplicationStopFailures;

  @Attributes(title = "Enable Rollbacks") private boolean enableAutoRollback;

  @EnumData(enumDataProvider = CodeDeployAutoRollbackConfigurationProvider.class)
  @Attributes(title = "Rollback configuration overrides")
  private List<String> autoRollbackConfigurations;

  @EnumData(enumDataProvider = CodeDeployFileExistBehaviorProvider.class)
  @DefaultValue("DISALLOW")
  @Attributes(title = "Content options")
  private String fileExistsBehavior;

  @Attributes(title = "Command") @DefaultValue("Amazon Code Deploy") private String commandName = "Amazon Code Deploy";

  @Inject @Transient private transient AwsCodeDeployHelperServiceManager awsCodeDeployHelperServiceManager;

  @Inject @Transient protected transient SettingsService settingsService;

  @Inject @Transient protected transient DelegateService delegateService;

  @Inject @Transient protected transient ServiceResourceService serviceResourceService;

  @Inject @Transient protected transient ActivityService activityService;

  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;

  @Inject @Transient private transient AwsHelperService awsHelperService;

  @Inject @Transient protected transient SecretManager secretManager;

  public AwsCodeDeployState(String name) {
    super(name, StateType.AWS_CODEDEPLOY_STATE.name());
  }

  protected AwsCodeDeployState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    String envId = env.getUuid();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    CodeDeployInfrastructureMapping infrastructureMapping =
        (CodeDeployInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    List<EncryptedDataDetail> encryptedDataDetails = secretManager.getEncryptionDetails(
        (EncryptableSetting) cloudProviderSetting.getValue(), context.getAppId(), context.getWorkflowExecutionId());

    Activity act = Activity.builder()
                       .applicationName(app.getName())
                       .environmentId(envId)
                       .environmentName(env.getName())
                       .environmentType(env.getEnvironmentType())
                       .serviceId(service.getUuid())
                       .serviceName(service.getName())
                       .commandName(command.getName())
                       .type(Type.Command)
                       .workflowExecutionId(context.getWorkflowExecutionId())
                       .workflowId(context.getWorkflowId())
                       .workflowType(context.getWorkflowType())
                       .workflowExecutionName(context.getWorkflowExecutionName())
                       .stateExecutionInstanceId(context.getStateExecutionInstanceId())
                       .stateExecutionInstanceName(context.getStateExecutionInstanceName())
                       .commandUnits(serviceResourceService.getFlattenCommandUnitList(
                           app.getUuid(), serviceId, envId, command.getName()))
                       .commandType(command.getCommandUnitType().name())
                       .status(ExecutionStatus.RUNNING)
                       .build();

    act.setAppId(app.getUuid());
    Activity activity = activityService.save(act);

    executionDataBuilder.withServiceId(service.getUuid())
        .withServiceName(service.getName())
        .withAppId(app.getUuid())
        .withCommandName(getCommandName())
        .withActivityId(activity.getUuid());

    CodeDeployParams codeDeployParams = prepareCodeDeployParams(
        context, infrastructureMapping, cloudProviderSetting, encryptedDataDetails, executionDataBuilder);

    CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                          .withAccountId(app.getAccountId())
                                                          .withAppId(app.getUuid())
                                                          .withEnvId(envId)
                                                          .withServiceName(service.getName())
                                                          .withDeploymentType(infrastructureMapping.getDeploymentType())
                                                          .withActivityId(activity.getUuid())
                                                          .withCloudProviderSetting(cloudProviderSetting)
                                                          .withCloudProviderCredentials(encryptedDataDetails)
                                                          .withCodeDeployParams(codeDeployParams)
                                                          .build();

    String delegateTaskId =
        delegateService.queueTask(aDelegateTask()
                                      .withAccountId(app.getAccountId())
                                      .withAppId(app.getAppId())
                                      .withTaskType(TaskType.COMMAND)
                                      .withWaitId(activity.getUuid())
                                      .withTimeout(getTaskTimeout())
                                      .withParameters(new Object[] {command, commandExecutionContext})
                                      .withEnvId(envId)
                                      .withInfrastructureMappingId(infrastructureMapping.getUuid())
                                      .build());

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(singletonList(activity.getUuid()))
        .withStateExecutionData(executionDataBuilder.build())
        .withDelegateTaskId(delegateTaskId)
        .build();
  }

  protected CodeDeployParams prepareCodeDeployParams(ExecutionContext context,
      CodeDeployInfrastructureMapping infrastructureMapping, SettingAttribute cloudProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, Builder executionDataBuilder) {
    CodeDeployParams codeDeployParams = CodeDeployParams.builder()
                                            .applicationName(infrastructureMapping.getApplicationName())
                                            .deploymentGroupName(infrastructureMapping.getDeploymentGroup())
                                            .region(infrastructureMapping.getRegion())
                                            .deploymentConfigurationName(infrastructureMapping.getDeploymentConfig())
                                            .bucket(context.renderExpression(bucket))
                                            .key(context.renderExpression(key))
                                            .bundleType(bundleType)
                                            .enableAutoRollback(enableAutoRollback)
                                            .autoRollbackConfigurations(autoRollbackConfigurations)
                                            .fileExistsBehavior(fileExistsBehavior)
                                            .ignoreApplicationStopFailures(ignoreApplicationStopFailures)
                                            .timeout(getTimeOut())
                                            .build();
    executionDataBuilder.withCodeDeployParams(codeDeployParams);

    AwsCodeDeployS3LocationData s3LocationData = awsCodeDeployHelperServiceManager.listAppRevision(
        (AwsConfig) cloudProviderSetting.getValue(), encryptedDataDetails, codeDeployParams.getRegion(),
        codeDeployParams.getApplicationName(), codeDeployParams.getDeploymentGroupName());
    if (s3LocationData != null) {
      CodeDeployParams oldCodeDeployParams =
          CodeDeployParams.builder()
              .applicationName(codeDeployParams.getApplicationName())
              .deploymentGroupName(codeDeployParams.getDeploymentGroupName())
              .deploymentConfigurationName(codeDeployParams.getDeploymentConfigurationName())
              .region(codeDeployParams.getRegion())
              .bucket(s3LocationData.getBucket())
              .key(s3LocationData.getKey())
              .bundleType(s3LocationData.getBundleType())
              .enableAutoRollback(enableAutoRollback)
              .autoRollbackConfigurations(autoRollbackConfigurations)
              .fileExistsBehavior(fileExistsBehavior)
              .ignoreApplicationStopFailures(ignoreApplicationStopFailures)
              .timeout(getTimeOut())
              .build();
      executionDataBuilder.withOldCodeDeployParams(oldCodeDeployParams);
    }
    return codeDeployParams;
  }

  private int getTimeOut() {
    return (steadyStateTimeout == 0) ? 10 : steadyStateTimeout;
  }

  private long getTaskTimeout() {
    long l1 = TimeUnit.HOURS.toMillis(1);
    long l2 = TimeUnit.MINUTES.toMillis(getTimeOut());
    return Math.max(l1, l2);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    CommandStateExecutionData commandStateExecutionData = (CommandStateExecutionData) context.getStateExecutionData();
    CommandExecutionResult commandExecutionResult = (CommandExecutionResult) response.values().iterator().next();

    ExecutionStatus status =
        commandExecutionResult != null && CommandExecutionStatus.SUCCESS.equals(commandExecutionResult.getStatus())
        ? ExecutionStatus.SUCCESS
        : ExecutionStatus.FAILED;
    activityService.updateStatus(
        commandStateExecutionData.getActivityId(), commandStateExecutionData.getAppId(), status);

    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();
    Key<ServiceTemplate> serviceTemplateKey =
        serviceTemplateService.getTemplateRefKeysByService(app.getUuid(), serviceId, env.getUuid()).get(0);

    CodeDeployInfrastructureMapping infrastructureMapping =
        (CodeDeployInfrastructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    InstanceElementListParam instanceElementListParam = anInstanceElementListParam().build();
    List<InstanceStatusSummary> instanceStatusSummaries = new ArrayList<>();
    if (commandExecutionResult != null && commandExecutionResult.getCommandExecutionData() != null) {
      CodeDeployCommandExecutionData commandExecutionData =
          (CodeDeployCommandExecutionData) commandExecutionResult.getCommandExecutionData();
      List<InstanceElement> instanceElements = new ArrayList<>();
      commandExecutionData.getInstances().forEach(instance -> {
        HostElement hostElement = aHostElement()
                                      .withPublicDns(instance.getPublicDnsName())
                                      .withIp(instance.getPrivateIpAddress())
                                      .withEc2Instance(instance)
                                      .withInstanceId(instance.getInstanceId())
                                      .build();

        final Map<String, Object> contextMap = context.asMap();
        contextMap.put("host", hostElement);
        String hostName =
            awsHelperService.getHostnameFromConvention(contextMap, infrastructureMapping.getHostNameConvention());

        hostElement.setHostName(hostName);
        InstanceElement instanceElement =
            anInstanceElement()
                .withUuid(instance.getInstanceId())
                .withHostName(hostName)
                .withDisplayName(instance.getPublicDnsName())
                .withHost(hostElement)
                .withServiceTemplateElement(aServiceTemplateElement()
                                                .withUuid(serviceTemplateKey.getId().toString())
                                                .withServiceElement(phaseElement.getServiceElement())
                                                .build())
                .build();
        instanceElements.add(instanceElement);

        instanceStatusSummaries.add(anInstanceStatusSummary()
                                        .withInstanceElement((InstanceElement) instanceElement.cloneMin())
                                        .withStatus(ExecutionStatus.SUCCESS)
                                        .build());
      });
      instanceElementListParam = anInstanceElementListParam().withInstanceElements(instanceElements).build();
      commandStateExecutionData.setNewInstanceStatusSummaries(instanceStatusSummaries);
      commandStateExecutionData.setCodeDeployDeploymentId(commandExecutionData.getDeploymentId());
      commandStateExecutionData.setDelegateMetaInfo(commandExecutionResult.getDelegateMetaInfo());
    }

    return anExecutionResponse()
        .withStateExecutionData(commandStateExecutionData)
        .withExecutionStatus(status)
        .addContextElement(instanceElementListParam)
        .addNotifyElement(instanceElementListParam)
        .build();
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();
    if (!isRollback()) {
      if (isBlank(bucket)) {
        invalidFields.put("bucket", "Bucket should not be empty");
      }
      if (isBlank(key)) {
        invalidFields.put("key", "Key should not be empty");
      }
      if (isBlank(bundleType)) {
        invalidFields.put("bundleType", "Bundle Type should not be empty");
      }
    }
    if (getCommandName() == null) {
      invalidFields.put("commandName", "Command Name should not be null");
    }
    return invalidFields;
  }

  @SchemaIgnore
  public String getCommandName() {
    return commandName;
  }

  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getBundleType() {
    return bundleType;
  }

  public void setBundleType(String bundleType) {
    this.bundleType = bundleType;
  }

  public boolean isIgnoreApplicationStopFailures() {
    return ignoreApplicationStopFailures;
  }

  public void setIgnoreApplicationStopFailures(boolean ignoreApplicationStopFailures) {
    this.ignoreApplicationStopFailures = ignoreApplicationStopFailures;
  }

  public String getFileExistsBehavior() {
    return fileExistsBehavior;
  }

  public void setFileExistsBehavior(String fileExistsBehavior) {
    this.fileExistsBehavior = fileExistsBehavior;
  }

  public boolean isEnableAutoRollback() {
    return enableAutoRollback;
  }

  public void setEnableAutoRollback(boolean enableAutoRollback) {
    this.enableAutoRollback = enableAutoRollback;
  }

  public List<String> getAutoRollbackConfigurations() {
    return autoRollbackConfigurations;
  }

  public void setAutoRollbackConfigurations(List<String> autoRollbackConfigurations) {
    this.autoRollbackConfigurations = autoRollbackConfigurations;
  }

  public int getSteadyStateTimeout() {
    return steadyStateTimeout;
  }

  public void setSteadyStateTimeout(int steadyStateTimeout) {
    this.steadyStateTimeout = steadyStateTimeout;
  }

  public static class CodeDeployAutoRollbackConfigurationProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, Map<String, String> params) {
      return ImmutableMap.of("DEPLOYMENT_FAILURE", "Roll back when a deployment fails", "DEPLOYMENT_STOP_ON_ALARM",
          "Roll back when alarm thresholds are met", "DEPLOYMENT_STOP_ON_REQUEST", "Roll back on request");
    }
  }

  public static class CodeDeployFileExistBehaviorProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, Map<String, String> params) {
      return ImmutableMap.of("DISALLOW", "Fail the deployment (Default option)", "OVERWRITE", "Overwrite the content",
          "RETAIN", "Retain the content");
    }
  }

  public static class CodeDeployBundleTypeProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, Map<String, String> params) {
      return ImmutableMap.of("tar", "A tar archive file (tar)", "tgz", "A compressed tar archive file (tgz)", "zip",
          "A zip archive file (zip)");
    }
  }

  public static Map<String, String> loadDefaults() {
    Map<String, String> stateDefaults = new HashMap<>();
    stateDefaults.put("bucket", ARTIFACT_S3_BUCKET_EXPRESSION);
    stateDefaults.put("key", ARTIFACT__S3_KEY_EXPRESSION);
    stateDefaults.put("bundleType", "zip");
    return stateDefaults;
  }
}
