package software.wings.sm.states.rancher;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.exception.ExceptionUtils.getMessage;

import static software.wings.sm.StateType.RANCHER_RESOLVE;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.tasks.ResponseData;

import software.wings.api.RancherClusterElement;
import software.wings.beans.*;
import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.RancherDummyCommandUnit;
import software.wings.common.RancherK8sClusterProcessor.RancherClusterElementList;
import software.wings.delegatetasks.rancher.RancherResolveClustersResponse;
import software.wings.delegatetasks.rancher.RancherResolveClustersTaskParameters;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.RancherKubernetesInfrastructure;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.*;
import software.wings.sm.states.utils.StateTimeoutUtils;
import software.wings.stencils.DefaultValue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import com.google.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(_870_CG_ORCHESTRATION)
@OwnedBy(CDP)
@BreakDependencyOn("software.wings.service.intfc.DelegateService")
public class RancherResolveState extends State {
  @Inject public RancherStateHelper rancherStateHelper;
  @Inject @Transient private SweepingOutputService sweepingOutputService;
  @Inject private transient SettingsService settingsService;
  @Inject private transient DelegateService delegateService;
  @Inject private transient InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private transient ActivityService activityService;
  @Inject private SecretManager secretManager;
  @Getter @Setter @Attributes(title = "Timeout (Minutes)") @DefaultValue("10") private Integer stateTimeoutInMinutes;

  public static final String COMMAND_UNIT_NAME = "Execute";
  public static final String COMMAND_NAME = "Rancher Cluster Resolve";
  private static final long MIN_TASK_TIMEOUT_IN_MINUTES = 1L;

  public RancherResolveState(String name) {
    super(name, RANCHER_RESOLVE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      Activity activity = createRancherActivity(context);

      RancherClusterElementList clusterElementList =
          sweepingOutputService.findSweepingOutput(context.prepareSweepingOutputInquiryBuilder()
                                                       .name(RancherClusterElementList.getSweepingOutputID(context))
                                                       .build());
      if (Objects.nonNull(clusterElementList)) {
        log.warn("Possible duplicate execution of Rancher Resolve within a workflow. Skipping this.");
        return ExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
      }

      return executeInternal(context, activity.getUuid());
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(getMessage(e), e);
    }
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    RancherKubernetesInfrastructureMapping rancherKubernetesInfrastructureMapping =
        rancherStateHelper.fetchRancherKubernetesInfrastructureMapping(context);

    InfrastructureDefinition infrastructureDefinition = infrastructureDefinitionService.getInfraDefById(
        context.getAccountId(), rancherKubernetesInfrastructureMapping.getInfrastructureDefinitionId());
    SettingAttribute settingAttribute =
        settingsService.get(rancherKubernetesInfrastructureMapping.getComputeProviderSettingId());
    RancherConfig rancherConfig = (RancherConfig) settingAttribute.getValue();

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(rancherConfig, context.getAppId(), context.getWorkflowExecutionId());

    RancherResolveClustersTaskParameters rancherResolveClustersTaskParameters =
        RancherResolveClustersTaskParameters.builder()
            .rancherConfig(rancherConfig)
            .encryptedDataDetails(encryptedDataDetails)
            .clusterSelectionCriteria(((RancherKubernetesInfrastructure) infrastructureDefinition.getInfrastructure())
                                          .getClusterSelectionCriteria())
            .activityId(activityId)
            .appId(context.getAppId())
            .build();

    String waitId = generateUuid();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(context.getApp().getAccountId())
                                    .setupAbstraction(Cd1SetupFields.APP_ID_FIELD, context.getApp().getUuid())
                                    .waitId(waitId)
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(TaskType.RANCHER_RESOLVE_CLUSTERS.name())
                                              .parameters(new Object[] {rancherResolveClustersTaskParameters})
                                              .timeout(getTimeoutValue() * 60 * 1000L)
                                              .build())
                                    .selectionLogsTrackingEnabled(true)
                                    .build();

    delegateService.queueTask(delegateTask);
    StateExecutionData executionData = new RancherStateExecutionData(activityId);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(waitId))
        .stateExecutionData(executionData)
        .build();
  }

  private long getTimeoutValue() {
    long timeoutMillis = DEFAULT_ASYNC_CALL_TIMEOUT;

    if (this.stateTimeoutInMinutes != null) {
      long taskTimeoutInMinutes;
      if (stateTimeoutInMinutes < MIN_TASK_TIMEOUT_IN_MINUTES) {
        taskTimeoutInMinutes = MIN_TASK_TIMEOUT_IN_MINUTES;
      } else {
        taskTimeoutInMinutes = stateTimeoutInMinutes;
      }

      timeoutMillis = taskTimeoutInMinutes * 60L * 1000L;
    }

    return timeoutMillis;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    RancherResolveClustersResponse executionResponse =
        (RancherResolveClustersResponse) response.values().iterator().next();

    activityService.updateStatus(((RancherStateExecutionData) context.getStateExecutionData()).getActivityId(),
        context.getAppId(), executionResponse.getExecutionStatus());

    if (ExecutionStatus.FAILED == executionResponse.getExecutionStatus()) {
      return ExecutionResponse.builder()
          .executionStatus(executionResponse.getExecutionStatus())
          .stateExecutionData(context.getStateExecutionData())
          .build();
    }
    List<String> clusterNames = executionResponse.getClusters();
    List<RancherClusterElement> clusterElements =
        clusterNames.stream()
            .map(name -> new RancherClusterElement(UUIDGenerator.generateUuid(), name))
            .collect(Collectors.toList());

    sweepingOutputService.save(context.prepareSweepingOutputBuilder(SweepingOutputInstance.Scope.WORKFLOW)
                                   .name(RancherClusterElementList.getSweepingOutputID(context))
                                   .value(new RancherClusterElementList(clusterElements))
                                   .build());

    return ExecutionResponse.builder().executionStatus(ExecutionStatus.SUCCESS).build();
  }

  @Override
  public Integer getTimeoutMillis() {
    return StateTimeoutUtils.getTimeoutMillisFromMinutes(stateTimeoutInMinutes);
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    // NoOp
  }

  private Activity createRancherActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();
    Environment env = ((ExecutionContextImpl) executionContext).fetchRequiredEnvironment();

    Activity activity = Activity.builder()
                            .applicationName(app.getName())
                            .appId(app.getUuid())
                            .commandName(COMMAND_NAME)
                            .type(Activity.Type.Command)
                            .workflowType(executionContext.getWorkflowType())
                            .workflowExecutionName(executionContext.getWorkflowExecutionName())
                            .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                            .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                            .commandType(getStateType())
                            .workflowExecutionId(executionContext.getWorkflowExecutionId())
                            .workflowId(executionContext.getWorkflowId())
                            .commandUnits(Collections.singletonList(new RancherDummyCommandUnit(COMMAND_UNIT_NAME)))
                            .status(ExecutionStatus.RUNNING)
                            .commandUnitType(CommandUnitDetails.CommandUnitType.RANCHER)
                            .environmentId(env.getUuid())
                            .environmentName(env.getName())
                            .environmentType(env.getEnvironmentType())
                            .build();

    return activityService.save(activity);
  }
}