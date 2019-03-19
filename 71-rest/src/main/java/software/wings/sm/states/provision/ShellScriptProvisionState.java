package software.wings.sm.states.provision;

import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.DEFAULT_ASYNC_CALL_TIMEOUT;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.shellscript.provision.ShellScriptProvisionExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.Environment;
import software.wings.beans.NameValuePair;
import software.wings.beans.SweepingOutput;
import software.wings.beans.TaskType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.shellscript.provisioner.ShellScriptInfrastructureProvisioner;
import software.wings.beans.shellscript.provisioner.ShellScriptProvisionParameters;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class ShellScriptProvisionState extends State implements SweepingOutputStateMixin {
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptProvisionState.class);
  private static final int TIMEOUT_IN_MINUTES = 20;
  private static final String COMMAND_UNIT = "Shell Script Provision";
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private ActivityService activityService;
  @Inject private DelegateService delegateService;
  @Inject private SweepingOutputService sweepingOutputService;
  @Getter @Setter private String provisionerId;
  @Getter @Setter private List<NameValuePair> variables;
  @Getter @Setter private String sweepingOutputName;
  @Getter @Setter private SweepingOutput.Scope sweepingOutputScope;

  public ShellScriptProvisionState(String name) {
    super(name, StateType.SHELL_SCRIPT_PROVISION.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    ShellScriptInfrastructureProvisioner shellScriptProvisioner =
        infrastructureProvisionerService.getShellScriptProvisioner(context.getAppId(), provisionerId);

    ShellScriptProvisionParameters parameters =
        ShellScriptProvisionParameters.builder()
            .scriptBody(shellScriptProvisioner.getScriptBody())
            .textVariables(infrastructureProvisionerService.extractTextVariables(variables, context))
            .encryptedVariables(
                infrastructureProvisionerService.extractEncryptedTextVariables(variables, context.getAppId()))
            .timeoutInMillis(TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES))
            .accountId(context.getAccountId())
            .appId(context.getAppId())
            .activityId(activityId)
            .commandUnit(COMMAND_UNIT)
            .entityId(infrastructureProvisionerService.getEntityId(
                provisionerId, ((ExecutionContextImpl) context).getEnv().getUuid()))
            .build();

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(context.getAccountId())
                                    .waitId(activityId)
                                    .appId(context.getAppId())
                                    .async(true)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.SHELL_SCRIPT_PROVISION_TASK.toString())
                                              .parameters(new Object[] {parameters})
                                              .timeout(defaultIfNullTimeout(DEFAULT_ASYNC_CALL_TIMEOUT))
                                              .build())
                                    .build();

    String delegateTaskId = delegateService.queueTask(delegateTask);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withDelegateTaskId(delegateTaskId)
        .withStateExecutionData(ScriptStateExecutionData.builder().activityId(activityId).build())
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();
    ShellScriptProvisionExecutionData executionData = (ShellScriptProvisionExecutionData) responseEntry.getValue();
    executionData.setActivityId(activityId);

    if (executionData.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      String output = executionData.getOutput();
      Map<String, Object> outputMap = parseOutput(output);
      infrastructureProvisionerService.regenerateInfrastructureMappings(provisionerId, context, outputMap);
      handleSweepingOutput(sweepingOutputService, context, outputMap);
    }

    activityService.updateStatus(activityId, context.getAppId(), executionData.getExecutionStatus());
    return ExecutionResponse.Builder.anExecutionResponse()
        .withStateExecutionData(executionData)
        .withExecutionStatus(executionData.getExecutionStatus())
        .withErrorMessage(executionData.getErrorMsg())
        .build();
  }

  @VisibleForTesting
  Map<String, Object> parseOutput(String output) {
    Map<String, Object> outputs = new LinkedHashMap<>();
    if (isBlank(output)) {
      return outputs;
    }

    TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
    try {
      return new ObjectMapper().readValue(output, typeRef);
    } catch (IOException e) {
      logger.error("Output : " + output);
      throw new InvalidRequestException("Not a json type output", WingsException.USER);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

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
            .commandUnits(
                asList(Command.Builder.aCommand().withName(COMMAND_UNIT).withCommandType(CommandType.OTHER).build()))
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
}
