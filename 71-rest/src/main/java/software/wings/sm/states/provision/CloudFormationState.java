package software.wings.sm.states.provision;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.Log.Builder.aLog;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.TriggeredBy;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.query.Query;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TemplateExpression;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.infrastructure.CloudFormationRollbackConfig;
import software.wings.beans.infrastructure.CloudFormationRollbackConfig.CloudFormationRollbackConfigKeys;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationRollbackInfo;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.stencils.DefaultValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@FieldNameConstants(innerTypeName = "CloudFormationStateKeys")
public abstract class CloudFormationState extends State {
  @Inject private transient ActivityService activityService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject protected transient DelegateService delegateService;
  @Inject protected transient SecretManager secretManager;
  @Inject protected transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject protected transient LogService logService;
  @Inject protected transient TemplateExpressionProcessor templateExpressionProcessor;
  @Inject protected transient WingsPersistence wingsPersistence;

  @Attributes(title = "Provisioner") @Getter @Setter protected String provisionerId;
  @Attributes(title = "Region") @DefaultValue("us-east-1") @Getter @Setter protected String region = "us-east-1";
  @Attributes(title = "AwsConfigId") @Getter @Setter protected String awsConfigId;
  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;
  @Attributes(title = "Use Custom Stack Name") @Getter @Setter protected boolean useCustomStackName;
  @Attributes(title = "Custom Stack Name") @Getter @Setter protected String customStackName;

  private static final int IDSIZE = 8;
  private static final Set<Character> ALLOWED_CHARS =
      Sets.newHashSet(Lists.charactersOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-"));

  public CloudFormationState(String name, String stateType) {
    super(name, stateType);
  }
  protected abstract String commandUnit();
  protected abstract DelegateTask buildDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId);
  protected abstract List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context);

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + TaskData.DEFAULT_ASYNC_CALL_TIMEOUT)
  @Override
  public Integer getTimeoutMillis() {
    return super.getTimeoutMillis();
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // nothing to do
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    String activityId = response.keySet().iterator().next();
    CloudFormationCommandExecutionResponse executionResponse =
        (CloudFormationCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    ExecutionResponseBuilder builder = ExecutionResponse.builder().executionStatus(executionStatus);
    if (ExecutionStatus.SUCCESS.equals(executionStatus)) {
      List<CloudFormationElement> elements = handleResponse(executionResponse.getCommandResponse(), context);
      if (isNotEmpty(elements)) {
        elements.forEach(element -> {
          builder.contextElement(element);
          builder.notifyElement(element);
        });
      }
    }
    return builder.build();
  }

  private CloudFormationInfrastructureProvisioner getProvisioner(ExecutionContext context) {
    InfrastructureProvisioner infrastructureProvisioner =
        infrastructureProvisionerService.get(context.getAppId(), provisionerId);

    if (!(infrastructureProvisioner instanceof CloudFormationInfrastructureProvisioner)) {
      throw new InvalidRequestException("");
    }
    return (CloudFormationInfrastructureProvisioner) infrastructureProvisioner;
  }

  protected AwsConfig getAwsConfig(String awsConfigId) {
    SettingAttribute awsSettingAttribute = settingsService.get(awsConfigId);
    notNullCheck("awsSettingAttribute", awsSettingAttribute);
    if (!(awsSettingAttribute.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("Setting attribute is of AwsConfig");
    }
    return (AwsConfig) awsSettingAttribute.getValue();
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    CloudFormationInfrastructureProvisioner cloudFormationInfrastructureProvisioner = getProvisioner(context);

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    AwsConfig awsConfig;
    final List<TemplateExpression> templateExpressions = getTemplateExpressions();
    if (isNotEmpty(templateExpressions)) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(templateExpressions, "awsConfigId");
      if (configIdExpression != null) {
        SettingAttribute settingAttribute = templateExpressionProcessor.resolveSettingAttributeByNameOrId(
            context, configIdExpression, SettingVariableTypes.AWS);
        awsConfig = (AwsConfig) settingAttribute.getValue();
      } else {
        awsConfig = getAwsConfig(awsConfigId);
      }
    } else {
      awsConfig = getAwsConfig(awsConfigId);
    }

    DelegateTask delegateTask =
        buildDelegateTask(executionContext, cloudFormationInfrastructureProvisioner, awsConfig, activityId);

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(activityId))
        .delegateTaskId(delegateTaskId)
        .stateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
        .build();
  }

  protected void setTimeOutOnRequest(CloudFormationCommandRequest request) {
    if (request != null) {
      Integer timeout = getTimeoutMillis();
      if (timeout != null) {
        request.setTimeoutInMs(timeout);
      }
    }
  }

  protected void updateInfraMappings(
      CloudFormationCommandResponse commandResponse, ExecutionContext context, String provisionerId) {
    CloudFormationCreateStackResponse createStackResponse = (CloudFormationCreateStackResponse) commandResponse;
    ScriptStateExecutionData scriptStateExecutionData = (ScriptStateExecutionData) context.getStateExecutionData();
    Log.Builder logBuilder = aLog()
                                 .withAppId(context.getAppId())
                                 .withActivityId(scriptStateExecutionData.getActivityId())
                                 .withLogLevel(LogLevel.INFO)
                                 .withCommandUnitName(commandUnit())
                                 .withExecutionResult(CommandExecutionStatus.RUNNING);

    ManagerExecutionLogCallback executionLogCallback =
        new ManagerExecutionLogCallback(logService, logBuilder, scriptStateExecutionData.getActivityId());

    if (CommandExecutionStatus.SUCCESS.equals(commandResponse.getCommandExecutionStatus())) {
      Map<String, Object> outputs = createStackResponse.getCloudFormationOutputMap();
      if (isNotEmpty(outputs)) {
        infrastructureProvisionerService.regenerateInfrastructureMappings(
            provisionerId, context, outputs, Optional.of(executionLogCallback), Optional.of(region));
      } else {
        executionLogCallback.saveExecutionLog(
            "There are not outputs in the CF Stack. No infra mappings would be updated.");
      }
    }
    executionLogCallback.saveExecutionLog("Completed CF Create Stack", LogLevel.INFO, CommandExecutionStatus.SUCCESS);
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    notNullCheck("workflowStandardParams", workflowStandardParams, USER);
    notNullCheck("currentUser", workflowStandardParams.getCurrentUser(), USER);

    ActivityBuilder activityBuilder =
        Activity.builder()
            .applicationName(app.getName())
            .commandName(getName())
            .type(Type.Command)
            .workflowType(executionContext.getWorkflowType())
            .workflowExecutionName(executionContext.getWorkflowExecutionName())
            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .commandType(getStateType())
            .workflowExecutionId(executionContext.getWorkflowExecutionId())
            .workflowId(executionContext.getWorkflowId())
            .commandUnits(Collections.singletonList(
                Builder.aCommand().withName(commandUnit()).withCommandType(CommandType.OTHER).build()))
            .status(ExecutionStatus.RUNNING)
            .triggeredBy(TriggeredBy.builder()
                             .email(workflowStandardParams.getCurrentUser().getEmail())
                             .name(workflowStandardParams.getCurrentUser().getName())
                             .build());

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType().equals(BUILD)) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }
    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  protected String getStackNameSuffix(ExecutionContextImpl executionContext, String provisionerId) {
    WorkflowStandardParams workflowStandardParams = executionContext.fetchWorkflowStandardParamsFromContext();
    Environment env = workflowStandardParams.fetchRequiredEnv();
    return getNormalizedId(env.getUuid()) + getNormalizedId(provisionerId);
  }

  protected String fetchResolvedAwsConfigId(ExecutionContext context) {
    if (isNotEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "awsConfigId");
      if (configIdExpression != null) {
        SettingAttribute settingAttribute = templateExpressionProcessor.resolveSettingAttributeByNameOrId(
            context, configIdExpression, SettingVariableTypes.AWS);
        return settingAttribute.getUuid();
      } else {
        return awsConfigId;
      }
    }
    return awsConfigId;
  }

  private String getNormalizedId(String id) {
    if (isEmpty(id)) {
      return id;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < IDSIZE && i < id.length(); i++) {
      char ch = id.charAt(i);
      sb.append(ALLOWED_CHARS.contains(ch) ? ch : '-');
    }
    return sb.toString();
  }

  protected void clearRollbackConfig(ExecutionContextImpl context) {
    Query<CloudFormationRollbackConfig> query =
        wingsPersistence.createQuery(CloudFormationRollbackConfig.class)
            .filter(CloudFormationRollbackConfigKeys.entityId, getStackNameSuffix(context, provisionerId));
    wingsPersistence.delete(query);
  }

  protected void saveCloudFormationRollbackConfig(
      CloudFormationRollbackInfo rollbackInfo, ExecutionContextImpl context, String awsConfigId) {
    String url = null;
    String body = null;
    String createType;
    if (isNotEmpty(rollbackInfo.getUrl())) {
      createType = CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_URL;
      url = rollbackInfo.getUrl();
    } else {
      createType = CloudFormationCreateStackRequest.CLOUD_FORMATION_STACK_CREATE_BODY;
      body = rollbackInfo.getBody();
    }
    wingsPersistence.save(CloudFormationRollbackConfig.builder()
                              .appId(context.getAppId())
                              .url(url)
                              .body(body)
                              .region(rollbackInfo.getRegion())
                              .awsConfigId(awsConfigId)
                              .customStackName(rollbackInfo.getCustomStackName())
                              .createType(createType)
                              .variables(rollbackInfo.getVariables())
                              .workflowExecutionId(context.getWorkflowExecutionId())
                              .entityId(getStackNameSuffix(context, provisionerId))
                              .build());
  }
}