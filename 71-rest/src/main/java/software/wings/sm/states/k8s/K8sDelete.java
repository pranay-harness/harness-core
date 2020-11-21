package software.wings.sm.states.k8s;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.sm.StateType.K8S_DELETE;
import static software.wings.sm.states.k8s.K8sStateHelper.getSafeTimeoutInMillis;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.ExecutionStatus;
import io.harness.context.ContextElementType;
import io.harness.data.validator.Trimmed;
import io.harness.delegate.task.k8s.K8sTaskType;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.tasks.ResponseData;

import software.wings.api.k8s.K8sStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.K8sDummyCommandUnit;
import software.wings.delegatetasks.aws.AwsCommandHelper;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class K8sDelete extends State implements K8sStateExecutor {
  @Inject private transient ConfigService configService;
  @Inject private transient ServiceTemplateService serviceTemplateService;
  @Inject private transient ActivityService activityService;
  @Inject private transient SecretManager secretManager;
  @Inject private transient SettingsService settingsService;
  @Inject private transient AppService appService;
  @Inject private transient InfrastructureMappingService infrastructureMappingService;
  @Inject private transient DelegateService delegateService;
  @Inject private ContainerDeploymentManagerHelper containerDeploymentManagerHelper;
  @Inject private transient K8sStateHelper k8sStateHelper;
  @Inject private transient ApplicationManifestService applicationManifestService;
  @Inject private transient AwsCommandHelper awsCommandHelper;

  public static final String K8S_DELETE_COMMAND_NAME = "Delete";

  public K8sDelete(String name) {
    super(name, K8S_DELETE.name());
  }

  @Getter @Setter @Attributes(title = "Resources") private String resources;
  @Getter @Setter private boolean deleteNamespacesForRelease;

  @Trimmed @Getter @Setter @Attributes(title = "File paths") private String filePaths;
  @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") @Getter @Setter private Integer stateTimeoutInMinutes;

  @Override
  public Integer getTimeoutMillis() {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(stateTimeoutInMinutes);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    if (needsManifest()) {
      return executeWithManifest(context);
    } else {
      return executeWithoutManifest(context);
    }
  }

  private boolean needsManifest() {
    return isNotEmpty(filePaths);
  }

  private ExecutionResponse executeWithManifest(ExecutionContext context) {
    return k8sStateHelper.executeWrapperWithManifest(this, context, getSafeTimeoutInMillis(getTimeoutMillis()));
  }

  private ExecutionResponse executeWithoutManifest(ExecutionContext context) {
    try {
      ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

      Activity activity = createActivity(context);

      K8sTaskParameters k8sTaskParameters = K8sDeleteTaskParameters.builder()
                                                .activityId(activity.getUuid())
                                                .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
                                                .commandName(K8S_DELETE_COMMAND_NAME)
                                                .k8sTaskType(K8sTaskType.DELETE)
                                                .resources(context.renderExpression(this.resources))
                                                .deleteNamespacesForRelease(deleteNamespacesForRelease)
                                                .timeoutIntervalInMin(10)
                                                .build();

      return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    return k8sStateHelper.handleAsyncResponseWrapper(this, context, response);
  }

  @Override
  public void validateParameters(ExecutionContext context) {
    // This function is not being used anywhere for k8sDelete but we need to have this because of the interface.
  }

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> invalidFields = new HashMap<>();

    if (isBlank(resources) && isBlank(filePaths)) {
      invalidFields.put("resources", "Resources must not be blank");
    }

    return invalidFields;
  }

  @Override
  public String commandName() {
    return K8S_DELETE_COMMAND_NAME;
  }

  @Override
  public String stateType() {
    return getStateType();
  }

  @Override
  public List<CommandUnit> commandUnitList(boolean remoteStoreType) {
    List<CommandUnit> applyCommandUnits = new ArrayList<>();

    if (remoteStoreType) {
      applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.FetchFiles));
    }

    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init));
    applyCommandUnits.add(new K8sDummyCommandUnit(K8sCommandUnitConstants.Delete));

    return applyCommandUnits;
  }

  @Override
  public ExecutionResponse executeK8sTask(ExecutionContext context, String activityId) {
    Map<K8sValuesLocation, ApplicationManifest> appManifestMap = k8sStateHelper.getApplicationManifests(context);
    ContainerInfrastructureMapping infraMapping = k8sStateHelper.getContainerInfrastructureMapping(context);

    renderStateVariables(context);
    K8sTaskParameters k8sTaskParameters;
    k8sTaskParameters = K8sDeleteTaskParameters.builder()
                            .activityId(activityId)
                            .releaseName(k8sStateHelper.getReleaseName(context, infraMapping))
                            .commandName(K8S_DELETE_COMMAND_NAME)
                            .k8sTaskType(K8sTaskType.DELETE)
                            .resources(resources)
                            .deleteNamespacesForRelease(deleteNamespacesForRelease)
                            .filePaths(filePaths)
                            .timeoutIntervalInMin(stateTimeoutInMinutes)
                            .k8sDelegateManifestConfig(k8sStateHelper.createDelegateManifestConfig(
                                context, appManifestMap.get(K8sValuesLocation.Service)))
                            .valuesYamlList(k8sStateHelper.getRenderedValuesFiles(appManifestMap, context))
                            .build();
    return k8sStateHelper.queueK8sDelegateTask(context, k8sTaskParameters);
  }

  @Override
  public ExecutionResponse handleAsyncResponseForK8sTask(ExecutionContext context, Map<String, ResponseData> response) {
    try {
      WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
      String appId = workflowStandardParams.getAppId();
      K8sTaskExecutionResponse executionResponse = (K8sTaskExecutionResponse) response.values().iterator().next();

      ExecutionStatus executionStatus = executionResponse.getCommandExecutionStatus() == CommandExecutionStatus.SUCCESS
          ? ExecutionStatus.SUCCESS
          : ExecutionStatus.FAILED;

      activityService.updateStatus(k8sStateHelper.getActivityId(context), appId, executionStatus);

      K8sStateExecutionData stateExecutionData = (K8sStateExecutionData) context.getStateExecutionData();
      stateExecutionData.setStatus(executionStatus);

      return ExecutionResponse.builder()
          .executionStatus(executionStatus)
          .stateExecutionData(context.getStateExecutionData())
          .build();
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void renderStateVariables(ExecutionContext context) {
    // If both resources and filePaths are present then the resources get priority over filePaths.
    if (isNotBlank(resources)) {
      resources = context.renderExpression(resources);
    }
    if (isNotBlank(filePaths)) {
      filePaths = context.renderExpression(filePaths);
    }
  }

  private Activity createActivity(ExecutionContext context) {
    return k8sStateHelper.createK8sActivity(context, K8S_DELETE_COMMAND_NAME, getStateType(), activityService,
        ImmutableList.of(new K8sDummyCommandUnit(K8sCommandUnitConstants.Init),
            new K8sDummyCommandUnit(K8sCommandUnitConstants.Delete)));
  }
}
