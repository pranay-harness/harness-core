package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.beans.activity.cd10.CD10RegisterActivityDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.cvng.state.CVNGVerificationTask.Status;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.tasks.ResponseData;

import software.wings.api.ExecutionDataValue;
import software.wings.api.instancedetails.InstanceApiResponse;
import software.wings.beans.WorkflowExecution;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;

@Data
@FieldNameConstants(innerTypeName = "CVNGStateKeys")
@Slf4j
@OwnedBy(CV)
public class CVNGState extends State {
  @VisibleForTesting static final String DEFAULT_HOSTNAME_TEMPLATE = "${instanceDetails.hostName}";
  private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMinutes(2);
  @SchemaIgnore @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Inject private CVNGService cvngService;
  @SchemaIgnore @Inject private WorkflowExecutionService workflowExecutionService;
  @SchemaIgnore @Inject private Clock clock;
  private String webhookUrl;
  private String deploymentTag;
  private String dataCollectionDelay;
  private String orgIdentifier;
  private String projectIdentifier;
  private String verificationJobIdentifier;
  private List<ParamValue> cvngParams;
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ParamValue {
    String name;
    String value;
    boolean editable;
  }

  public CVNGState(String name) {
    super(name, StateType.CVNG.name());
  }
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      Instant now = clock.instant();
      WorkflowExecution workflowExecution =
          workflowExecutionService.getWorkflowExecution(context.getAppId(), context.getWorkflowExecutionId());
      CVInstanceApiResponse cvInstanceApiResponse = getCVInstanceAPIResponse(context);
      DeploymentActivityDTO activityDTO =
          DeploymentActivityDTO.builder()
              .deploymentTag(getDeploymentTag(context))
              .accountIdentifier(context.getAccountId())
              .orgIdentifier(orgIdentifier)
              .projectIdentifier(projectIdentifier)
              .activityStartTime(workflowExecution.getStartTs())
              .verificationStartTime(now.toEpochMilli())
              .name(context.getWorkflowExecutionName())
              .oldVersionHosts(cvInstanceApiResponse.getOldVersionHosts())
              .newVersionHosts(cvInstanceApiResponse.getNewVersionHosts())
              .dataCollectionDelayMs(getDataCollectionDelay(context))
              .newHostsTrafficSplitPercentage(cvInstanceApiResponse.getNewNodesTrafficShiftPercent().orElse(null))
              .verificationJobRuntimeDetails(
                  Collections.singletonList(ActivityDTO.VerificationJobRuntimeDetails.builder()
                                                .verificationJobIdentifier(verificationJobIdentifier)
                                                .runtimeValues(getRuntimeValues(context, workflowExecution))
                                                .build()))
              .build();

      CD10RegisterActivityDTO cd10RegisterActivityDTO =
          cvngService.registerActivity(context.getAccountId(), activityDTO);
      String correlationId = UUID.randomUUID().toString();

      CVNGStateExecutionData cvngStateExecutionData =
          CVNGStateExecutionData.builder()
              .stateExecutionInstanceId(context.getStateExecutionInstanceId())
              .projectIdentifier(projectIdentifier)
              .orgIdentifier(orgIdentifier)
              .deploymentTag(getDeploymentTag(context))
              .serviceIdentifier(cd10RegisterActivityDTO.getServiceIdentifier())
              .activityId(cd10RegisterActivityDTO.getActivityId())
              .deploymentTag(getDeploymentTag(context))
              .envIdentifier(cd10RegisterActivityDTO.getEnvIdentifier())
              .serviceIdentifier(cd10RegisterActivityDTO.getServiceIdentifier())
              .orgIdentifier(orgIdentifier)
              .projectIdentifier(projectIdentifier)
              .build();
      cvngStateExecutionData.setStatus(ExecutionStatus.RUNNING);
      CVNGVerificationTask cvngVerificationTask = CVNGVerificationTask.builder()
                                                      .accountId(context.getAccountId())
                                                      .status(Status.IN_PROGRESS)
                                                      .activityId(cd10RegisterActivityDTO.getActivityId())
                                                      .startTime(now)
                                                      .correlationId(correlationId)
                                                      .build();
      cvngVerificationTaskService.create(cvngVerificationTask);
      return ExecutionResponse.builder()
          .async(true)
          .correlationIds(Collections.singletonList(correlationId))
          .executionStatus(ExecutionStatus.RUNNING)
          .stateExecutionData(cvngStateExecutionData)
          .build();
    } catch (Exception e) {
      return ExecutionResponse.builder()
          .async(false)
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  public String getValue(ExecutionContext context, String fieldName) {
    Optional<ParamValue> optionalParam =
        cvngParams.stream().filter(param -> param.getName().equals(fieldName)).findAny();
    if (optionalParam.isPresent()) {
      return context.renderExpression(optionalParam.get().getValue());
    } else {
      return null;
    }
  }

  public Long getDataCollectionDelay(ExecutionContext context) {
    if (dataCollectionDelay == null) {
      return null;
    }
    String delayMinutes = context.renderExpression(dataCollectionDelay);
    Preconditions.checkState(delayMinutes.charAt(delayMinutes.length() - 1) == 'm',
        "Data Collection Delay is not formatted correctly. Ex: use 10m for 10 minutes.");
    return TimeUnit.MINUTES.toMillis(Long.parseLong(delayMinutes.substring(0, delayMinutes.length() - 1)));
  }

  private String getDeploymentTag(ExecutionContext executionContext) {
    return executionContext.renderExpression(this.deploymentTag);
  }
  private Map<String, String> getRuntimeValues(ExecutionContext context, WorkflowExecution workflowExecution) {
    Map<String, String> runtimeValues = new HashMap<>();
    for (ParamValue param : this.cvngParams) {
      if (param.isEditable()) {
        runtimeValues.put(param.getName(), context.renderExpression(param.getValue()));
      }
    }
    Preconditions.checkState(workflowExecution.getServiceIds().size() == 1, "WorkflowExecution serviceIds: %s",
        workflowExecution.getServiceIds());
    String serviceId = workflowExecution.getServiceIds().get(0);
    String envId = workflowExecution.getEnvId();
    String appId = context.getAppId();
    runtimeValues.put("harnessCdAppId", appId);
    runtimeValues.put("harnessCdServiceId", serviceId);
    runtimeValues.put("harnessCdEnvId", envId);
    return runtimeValues;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext executionContext, Map<String, ResponseData> response) {
    Preconditions.checkState(response.size() == 1, "Should only have one element");
    CVNGStateResponseData responseData = (CVNGStateResponseData) response.values().iterator().next();
    CVNGStateExecutionData cvngStateExecutionData = executionContext.getStateExecutionData();
    cvngStateExecutionData.setStatus(responseData.getExecutionStatus());
    return ExecutionResponse.builder()
        .executionStatus(responseData.getExecutionStatus())
        .stateExecutionData(cvngStateExecutionData)
        .build();
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "CVNGStateExecutionDataKeys")
  public static class CVNGStateExecutionData extends StateExecutionData {
    private String activityId;
    private String projectIdentifier;
    private String orgIdentifier;
    private String deploymentTag;
    private String serviceIdentifier;
    private String stateExecutionInstanceId;
    private String envIdentifier;

    @Override
    public Map<String, ExecutionDataValue> getExecutionSummary() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionSummary();
      setExecutionData(executionDetails);
      return executionDetails;
    }

    @Override
    public Map<String, ExecutionDataValue> getExecutionDetails() {
      Map<String, ExecutionDataValue> executionDetails = super.getExecutionDetails();
      setExecutionData(executionDetails);
      return executionDetails;
    }

    private void setExecutionData(Map<String, ExecutionDataValue> executionDetails) {
      Map<String, String> idsMap = new HashMap<>();
      idsMap.put(CVNGStateExecutionDataKeys.activityId, activityId);
      idsMap.put(CVNGStateExecutionDataKeys.orgIdentifier, orgIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.projectIdentifier, projectIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.envIdentifier, envIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.serviceIdentifier, serviceIdentifier);
      idsMap.put(CVNGStateExecutionDataKeys.deploymentTag, deploymentTag);

      putNotNull(
          executionDetails, "cvngIds", ExecutionDataValue.builder().displayName("cvngIds").value(idsMap).build());
    }
  }

  @Value
  public static class CVNGStateResponseData implements ResponseData {
    private String activityId;
    private String correlationId;
    private ActivityStatusDTO activityStatusDTO;
    private Status status;
    private ExecutionStatus executionStatus;
    @Builder
    public CVNGStateResponseData(String activityId, String correlationId, ActivityStatusDTO activityStatusDTO,
        Status status, ExecutionStatus executionStatus) {
      this.activityId = activityId;
      this.correlationId = correlationId;
      this.activityStatusDTO = activityStatusDTO;
      this.status = status;
      if (executionStatus == null) {
        this.executionStatus = setExecutionUsingStatus();
      } else {
        this.executionStatus = executionStatus;
      }
    }

    private ExecutionStatus setExecutionUsingStatus() {
      switch (activityStatusDTO.getStatus()) {
        case VERIFICATION_PASSED:
          return ExecutionStatus.SUCCESS;
        case IN_PROGRESS:
        case NOT_STARTED:
          return ExecutionStatus.RUNNING;
        case VERIFICATION_FAILED:
          return ExecutionStatus.FAILED;
        case ERROR:
          return ExecutionStatus.ERROR;
        default:
          throw new IllegalStateException("Unhandled status value: " + activityStatusDTO.getStatus());
      }
    }
  }

  protected CVInstanceApiResponse getCVInstanceAPIResponse(ExecutionContext context) {
    Set<String> controlNodes, testNodes;
    Optional<Integer> newNodesTrafficShift;
    InstanceApiResponse allNodesResponse =
        context.renderExpressionsForInstanceDetailsForWorkflow(DEFAULT_HOSTNAME_TEMPLATE, false);
    Set<String> allNodes = new HashSet<>(allNodesResponse.getInstances());
    InstanceApiResponse instanceApiResponse =
        context.renderExpressionsForInstanceDetails(DEFAULT_HOSTNAME_TEMPLATE, true);
    testNodes = new HashSet<>(instanceApiResponse.getInstances());
    newNodesTrafficShift = instanceApiResponse.getNewInstanceTrafficPercent();
    Set<String> allPhaseNewNodes = new HashSet<>(
        context.renderExpressionsForInstanceDetailsForWorkflow(DEFAULT_HOSTNAME_TEMPLATE, true).getInstances());
    controlNodes = Sets.difference(allNodes, allPhaseNewNodes);
    return CVInstanceApiResponse.builder()
        .oldVersionHosts(controlNodes)
        .newVersionHosts(testNodes)
        .newNodesTrafficShiftPercent(newNodesTrafficShift)
        .build();
  }
  @Value
  @FieldNameConstants(innerTypeName = "CVInstanceApiResponseKeys")
  @Builder
  protected static class CVInstanceApiResponse {
    private Set<String> oldVersionHosts;
    private Set<String> newVersionHosts;
    private Optional<Integer> newNodesTrafficShiftPercent;
  }

  public enum StepStatus { SUCCESS, FAILED }
}
