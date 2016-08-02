package software.wings.sm.states;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.Release.Builder.aRelease;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.CommandUnit.ExecutionResult.ExecutionResultData.Builder.anExecutionResultData;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateType.COMMAND;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.InstanceElement;
import software.wings.api.SimpleWorkflowParam;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.Artifact;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Release;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.SettingAttribute;
import software.wings.beans.StringValue;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.command.CommandUnit.ExecutionResult.ExecutionResultData;
import software.wings.common.cache.ResponseCodeCache;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.stencils.Expand;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Created by peeyushaggarwal on 5/31/16.
 */
public class CommandState extends State {
  /**
   * The constant RUNTIME_PATH.
   */
  public static final String RUNTIME_PATH = "RUNTIME_PATH";
  /**
   * The constant BACKUP_PATH.
   */
  public static final String BACKUP_PATH = "BACKUP_PATH";
  /**
   * The constant STAGING_PATH.
   */
  public static final String STAGING_PATH = "STAGING_PATH";

  private static final Logger logger = LoggerFactory.getLogger(CommandState.class);

  @Inject @Transient private transient ServiceResourceService serviceResourceService;

  @Inject @Transient private transient ServiceInstanceService serviceInstanceService;

  @Inject @Transient private transient ServiceCommandExecutorService serviceCommandExecutorService;

  @Inject @Transient private transient ActivityService activityService;

  @Inject @Transient private transient SettingsService settingsService;

  @Inject @Transient private transient EnvironmentService environmentService;

  @Inject @Transient private transient WorkflowService workflowService;

  @Inject @Transient private ReleaseService releaseService;

  @Inject @Transient @SchemaIgnore private transient ExecutorService executorService;

  @Inject private transient WaitNotifyEngine waitNotifyEngine;

  @Attributes(title = "Command")
  @Expand(dataProvider = CommandStateDataProvider.class)
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  private String commandName;

  /**
   * Instantiates a new Command state.
   *
   * @param name        the name
   * @param commandName the command name
   */
  public CommandState(String name, String commandName) {
    super(name, COMMAND.name());
    this.commandName = commandName;
    this.setRequiredContextElementType(ContextElementType.INSTANCE);
  }

  /**
   * Instantiates a new Command state.
   *
   * @param name the name
   */
  public CommandState(String name) {
    super(name, COMMAND.name());
    this.setRequiredContextElementType(ContextElementType.INSTANCE);
  }

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData();
    String activityId = null;

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();
    Environment environment = environmentService.get(appId, envId, false);

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);

    updateWorflowExecutionStatsInProgress(context);

    try {
      if (instanceElement == null) {
        throw new StateExecutionException("No InstanceElement present in context");
      }

      ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

      if (serviceInstance == null) {
        throw new StateExecutionException("Unable to find service instance");
      }

      Service service = serviceInstance.getServiceTemplate().getService();

      executionDataBuilder.withServiceId(service.getUuid())
          .withServiceName(service.getName())
          .withTemplateId(serviceInstance.getServiceTemplate().getUuid())
          .withTemplateName(serviceInstance.getServiceTemplate().getName())
          .withHostId(serviceInstance.getHost().getInfraId())
          .withHostName(serviceInstance.getHost().getHostName())
          .withAppId(appId);

      String actualCommand = commandName;
      try {
        actualCommand = context.renderExpression(commandName);
      } catch (Exception e) {
        e.printStackTrace();
      }

      Command command = serviceResourceService.getCommandByName(appId, service.getUuid(), actualCommand);

      if (command == null) {
        throw new StateExecutionException(
            String.format("Unable to find command %s for service %s", actualCommand, service.getName()));
      }

      executionDataBuilder.withCommandName(command.getName())
          .withTotalCommandUnits(command.getCommandUnits().size() + 1);

      Activity.Builder activityBuilder = anActivity()
                                             .withAppId(serviceInstance.getAppId())
                                             .withEnvironmentId(environment.getUuid())
                                             .withEnvironmentName(environment.getName())
                                             .withEnvironmentType(environment.getEnvironmentType())
                                             .withServiceTemplateId(serviceInstance.getServiceTemplate().getUuid())
                                             .withServiceTemplateName(serviceInstance.getServiceTemplate().getName())
                                             .withServiceId(service.getUuid())
                                             .withServiceName(service.getName())
                                             .withCommandName(command.getName())
                                             .withCommandType(command.getCommandUnitType().name())
                                             .withHostName(serviceInstance.getHost().getHostName());

      String backupPath = getEvaluatedSettingValue(context, appId, envId, BACKUP_PATH).replace(" ", "\\ ");
      String runtimePath = getEvaluatedSettingValue(context, appId, envId, RUNTIME_PATH).replace(" ", "\\ ");
      String stagingPath = getEvaluatedSettingValue(context, appId, envId, STAGING_PATH).replace(" ", "\\ ");

      CommandExecutionContext.Builder commandExecutionContextBuilder =
          aCommandExecutionContext()
              .withAppId(appId)
              .withServiceInstance(serviceInstance)
              .withBackupPath(backupPath)
              .withRuntimePath(runtimePath)
              .withStagingPath(stagingPath)
              .withExecutionCredential(workflowStandardParams.getExecutionCredential());

      if (command.isArtifactNeeded()) {
        Artifact artifact =
            workflowStandardParams.getArtifactForService(serviceInstance.getServiceTemplate().getService());
        if (artifact == null) {
          throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
        }
        activityBuilder.withReleaseId(artifact.getRelease().getUuid())
            .withReleaseName(artifact.getRelease().getReleaseName())
            .withArtifactName(artifact.getDisplayName())
            .withArtifactId(artifact.getUuid());
        commandExecutionContextBuilder.withArtifact(artifact);
        executionDataBuilder.withArtifactName(artifact.getDisplayName()).withActivityId(artifact.getUuid());
      }

      Activity activity = activityService.save(activityBuilder.build());
      activityId = activity.getUuid();

      executionDataBuilder.withActivityId(activityId);

      String finalActivityId = activityId;
      executorService.execute(() -> {
        ExecutionResult executionResult = ExecutionResult.SUCCESS;
        String errorMessage = null;
        try {
          CommandExecutionContext commandExecutionContext =
              commandExecutionContextBuilder.withActivityId(finalActivityId).build();
          executionResult = serviceCommandExecutorService.execute(serviceInstance, command, commandExecutionContext);
        } catch (Exception e) {
          logger.warn("Exception: ", e);
          if (e instanceof WingsException) {
            WingsException ex = (WingsException) e;
            errorMessage =
                Joiner.on(",").join(ex.getResponseMessageList()
                                        .stream()
                                        .map(responseMessage
                                            -> ResponseCodeCache.getInstance()
                                                   .getResponseMessage(responseMessage.getCode(), ex.getParams())
                                                   .getMessage())
                                        .collect(toList()));
          } else {
            errorMessage = e.getMessage();
          }
          executionResult = ExecutionResult.FAILURE;
        }
        waitNotifyEngine.notify(finalActivityId,
            anExecutionResultData().withResult(executionResult).withErrorMessage(errorMessage).build());
      });
    } catch (Exception e) {
      handleCommandException(context, activityId, appId);
      updateWorkflowExecutionStats(ExecutionStatus.FAILED, context);
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.FAILED)
          .withStateExecutionData(executionDataBuilder.build())
          .withErrorMessage(e.getMessage())
          .build();
    }

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(activityId))
        .withStateExecutionData(executionDataBuilder.build())
        .build();
  }

  private void handleCommandException(ExecutionContext context, String activityId, String appId) {
    if (activityId != null) {
      activityService.updateStatus(activityId, appId, Status.FAILED);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String appId = workflowStandardParams.getAppId();
    String envId = workflowStandardParams.getEnvId();

    InstanceElement instanceElement = context.getContextElement(ContextElementType.INSTANCE);
    ServiceInstance serviceInstance = serviceInstanceService.get(appId, envId, instanceElement.getUuid());

    ExecutionResultData executionResultData = null;
    String activityId = null;
    for (Object status : response.values()) {
      executionResultData = ((ExecutionResultData) status);
    }

    for (String key : response.keySet()) {
      activityId = key;
    }

    if (executionResultData.getResult() != SUCCESS && isNotEmpty(executionResultData.getErrorMessage())) {
      handleCommandException(context, activityId, appId);
    }

    activityService.updateStatus(
        activityId, appId, executionResultData.getResult().equals(SUCCESS) ? Status.COMPLETED : Status.FAILED);

    Activity activity = activityService.get(activityId, appId);
    if (executionResultData.getResult().equals(SUCCESS)) {
      serviceInstance.setLastDeployedOn(currentTimeMillis());
      if (isNotEmpty(activity.getArtifactId()) || isNotEmpty(activity.getReleaseId())) {
        Release oldRelease = serviceInstance.getRelease();
        serviceInstance.setRelease(aRelease().withUuid(activity.getReleaseId()).build());
        serviceInstance.setArtifact(anArtifact().withUuid(activity.getArtifactId()).build());

        if (oldRelease == null || !StringUtils.equals(oldRelease.getUuid(), activity.getReleaseId())) {
          releaseService.addSuccessCount(appId, activity.getReleaseId(), envId, 1);
        }
      }
      serviceInstanceService.update(serviceInstance);
    }

    ExecutionStatus executionStatus =
        executionResultData.getResult().equals(SUCCESS) ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    updateWorkflowExecutionStats(executionStatus, context);

    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withErrorMessage(executionResultData.getErrorMessage())
        .build();
  }

  /**
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void updateWorkflowExecutionStats(ExecutionStatus executionStatus, ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      String appId = getAppId(context);
      if (executionStatus == ExecutionStatus.FAILED) {
        workflowService.incrementFailed(appId, context.getWorkflowExecutionId(), 1);
      } else {
        workflowService.incrementSuccess(appId, context.getWorkflowExecutionId(), 1);
      }
    }
  }

  private String getAppId(ExecutionContext context) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    return workflowStandardParams.getAppId();
  }

  private void updateWorflowExecutionStatsInProgress(ExecutionContext context) {
    Optional<ContextElement> simpleWorkflowParamOpt =
        context.getContextElementList(ContextElementType.PARAM)
            .stream()
            .filter(contextElement -> contextElement instanceof SimpleWorkflowParam)
            .findFirst();
    if (simpleWorkflowParamOpt.isPresent()) {
      String appId = getAppId(context);
      workflowService.incrementInProgressCount(appId, context.getWorkflowExecutionId(), 1);
    }
  }

  private String getEvaluatedSettingValue(ExecutionContext context, String appId, String envId, String variable) {
    SettingAttribute settingAttribute = settingsService.getByName(appId, envId, variable);
    StringValue stringValue = (StringValue) settingAttribute.getValue();
    String settingValue = stringValue.getValue();
    try {
      settingValue = context.renderExpression(settingValue);
    } catch (Exception e) {
      // ignore
    }
    return settingValue;
  }

  @Override
  @SchemaIgnore
  public List<EntityType> getRequiredExecutionArgumentTypes() {
    return Lists.newArrayList(EntityType.SERVICE, EntityType.SSH_USER, EntityType.SSH_PASSWORD, EntityType.ARTIFACT);
  }

  /**
   * Gets executor service.
   *
   * @return the executor service
   */
  @SchemaIgnore
  public ExecutorService getExecutorService() {
    return executorService;
  }

  /**
   * Sets executor service.
   *
   * @param executorService the executor service
   */
  @SchemaIgnore
  public void setExecutorService(ExecutorService executorService) {
    this.executorService = executorService;
  }
}
