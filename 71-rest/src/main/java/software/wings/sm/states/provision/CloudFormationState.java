package software.wings.sm.states.provision;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.OrchestrationWorkflowType.BUILD;
import static software.wings.common.Constants.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import io.harness.exception.InvalidRequestException;
import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.cloudformation.CloudFormationElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationInfrastructureProvisioner;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Log;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandType;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCommandRequest;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandExecutionResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCommandResponse;
import software.wings.helpers.ext.cloudformation.response.CloudFormationCreateStackResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.ManagerExecutionLogCallback;
import software.wings.stencils.DefaultValue;
import software.wings.utils.Validator;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class CloudFormationState extends State {
  @Inject @Transient private transient ActivityService activityService;
  @Inject @Transient protected transient DelegateService delegateService;
  @Inject @Transient private transient SettingsService settingsService;
  @Inject @Transient private transient AppService appService;
  @Inject @Transient protected transient SecretManager secretManager;
  @Inject @Transient protected transient InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject @Transient protected transient LogService logService;

  @Attributes(title = "Provisioner") @Getter @Setter protected String provisionerId;
  @Attributes(title = "Region") @DefaultValue("us-east-1") @Getter @Setter protected String region = "us-east-1";
  @Attributes(title = "AwsConfigId") @Getter @Setter protected String awsConfigId;
  @Attributes(title = "Variables") @Getter @Setter private List<NameValuePair> variables;

  private static final int IDSIZE = 8;
  private static final Set<Character> ALLOWED_CHARS =
      Sets.newHashSet(Lists.charactersOf("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-"));

  public CloudFormationState(String name, String stateType) {
    super(name, stateType);
  }
  protected abstract String commandUnit();
  protected abstract DelegateTask getDelegateTask(ExecutionContextImpl executionContext,
      CloudFormationInfrastructureProvisioner provisioner, AwsConfig awsConfig, String activityId);
  protected abstract List<CloudFormationElement> handleResponse(
      CloudFormationCommandResponse commandResponse, ExecutionContext context);

  @Attributes(title = "Timeout (ms)")
  @DefaultValue("" + DEFAULT_ASYNC_CALL_TIMEOUT)
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
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    String activityId = response.keySet().iterator().next();
    CloudFormationCommandExecutionResponse executionResponse =
        (CloudFormationCommandExecutionResponse) response.values().iterator().next();
    ExecutionStatus executionStatus =
        executionResponse.getCommandExecutionStatus().equals(CommandExecutionStatus.SUCCESS) ? ExecutionStatus.SUCCESS
                                                                                             : ExecutionStatus.FAILED;
    activityService.updateStatus(activityId, context.getAppId(), executionStatus);
    List<CloudFormationElement> elements = handleResponse(executionResponse.getCommandResponse(), context);
    ExecutionResponse.Builder builder = anExecutionResponse().withExecutionStatus(executionStatus);
    if (isNotEmpty(elements)) {
      elements.forEach(element -> {
        builder.addContextElement(element);
        builder.addNotifyElement(element);
      });
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
    Validator.notNullCheck("awsSettingAttribute", awsSettingAttribute);
    if (!(awsSettingAttribute.getValue() instanceof AwsConfig)) {
      throw new InvalidRequestException("");
    }
    return (AwsConfig) awsSettingAttribute.getValue();
  }

  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    CloudFormationInfrastructureProvisioner cloudFormationInfrastructureProvisioner = getProvisioner(context);

    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;
    DelegateTask delegateTask = getDelegateTask(
        executionContext, cloudFormationInfrastructureProvisioner, getAwsConfig(awsConfigId), activityId);
    if (getTimeoutMillis() != null) {
      delegateTask.setTimeout(getTimeoutMillis());
    }

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
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
      infrastructureProvisionerService.regenerateInfrastructureMappings(
          provisionerId, context, outputs, Optional.of(executionLogCallback), Optional.of(region));
    }
  }

  protected Map<String, String> getVariableMap(
      CloudFormationInfrastructureProvisioner cloudFormationInfrastructureProvisioner, ExecutionContext context) {
    Map<String, String> result = new HashMap<>();

    if (isNotEmpty(cloudFormationInfrastructureProvisioner.getVariables())) {
      for (NameValuePair entry : cloudFormationInfrastructureProvisioner.getVariables()) {
        result.put(entry.getName(), context.renderExpression(entry.getValue()));
      }
    }

    if (isNotEmpty(variables)) {
      for (NameValuePair entry : variables) {
        result.put(entry.getName(), context.renderExpression(entry.getValue()));
      }
    }
    return result;
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();

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
            .serviceVariables(Maps.newHashMap())
            .status(ExecutionStatus.RUNNING);

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
    WorkflowStandardParams workflowStandardParams = executionContext.getContextElement(ContextElementType.STANDARD);
    Environment env = workflowStandardParams.getEnv();
    return getNormalizedId(env.getUuid()) + getNormalizedId(provisionerId);
  }

  private String getNormalizedId(String id) {
    if (isEmpty(id)) {
      return id;
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < IDSIZE; i++) {
      char ch = id.charAt(i);
      sb.append(ALLOWED_CHARS.contains(ch) ? ch : '-');
    }
    return sb.toString();
  }
}