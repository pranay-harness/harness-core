package software.wings.sm.states;

import io.harness.beans.ExecutionStatus;
import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityStatusDTO;
import io.harness.cvng.beans.activity.DeploymentActivityDTO;
import io.harness.cvng.client.CVNGService;
import io.harness.cvng.state.CVNGVerificationTask;
import io.harness.cvng.state.CVNGVerificationTask.Status;
import io.harness.cvng.state.CVNGVerificationTaskService;
import io.harness.tasks.ResponseData;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
@Data
@Slf4j
public class CVNGState extends State {
  @SchemaIgnore @Inject private CVNGVerificationTaskService cvngVerificationTaskService;
  @Inject private CVNGService cvngService;
  private String deploymentTag;
  private String duration;
  private String webhookURL;
  private long deploymentTime;
  private long initialDelay;
  private String envIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String verificationJobIdentifier;

  public CVNGState(String name) {
    super(name, StateType.CVNG.name());
  }
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    try {
      long now = System.currentTimeMillis();
      ActivityDTO activityDTO = DeploymentActivityDTO.builder()
                                    .deploymentTag(deploymentTag)
                                    .environmentIdentifier(envIdentifier)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .activityStartTime(now)
                                    .verificationStartTime(now + initialDelay)
                                    .name(context.getWorkflowExecutionName())
                                    .accountIdentifier(context.getAccountId())
                                    .verificationJobRuntimeDetails(Collections.singletonList(
                                        ActivityDTO.VerificationJobRuntimeDetails.builder()
                                            .verificationJobIdentifier(verificationJobIdentifier)
                                            .runtimeValues(Collections.emptyMap())
                                            .build()))
                                    .build();

      String activityId = cvngService.registerActivity(context.getAccountId(), activityDTO);
      String correlationId = UUID.randomUUID().toString();
      CVNGStateExecutionData cvngStateExecutionData =
          CVNGStateExecutionData.builder()
              .stateExecutionInstanceId(context.getStateExecutionInstanceId())
              .activityId(activityId)
              .status(ExecutionStatus.RUNNING)
              .build();
      CVNGVerificationTask cvngVerificationTask = CVNGVerificationTask.builder()
                                                      .accountId(context.getAccountId())
                                                      .status(Status.IN_PROGRESS)
                                                      .activityId(activityId)
                                                      .startTime(Instant.ofEpochMilli(now))
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
  public static class CVNGStateExecutionData extends StateExecutionData {
    private String activityId;
    private String correlationId;
    private ExecutionStatus status;
    private String stateExecutionInstanceId;
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
}
