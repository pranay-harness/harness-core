package software.wings.sm.states;

import io.harness.beans.ExecutionStatus;
import io.harness.cvng.beans.ActivityDTO;
import io.harness.cvng.beans.DeploymentActivityDTO;
import io.harness.cvng.client.CVNGServiceClient;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;
@Data
@Slf4j
public class CVNGState extends State {
  private String deploymentTag;
  private String duration;
  private String webhookURL;
  private long deploymentTime;
  private long initialDelay;
  private String envIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String verificationJobIdentifier;

  @Inject private CVNGServiceClient cvngServiceClient;

  public CVNGState(String name) {
    super(name, StateType.CVNG.name());
  }
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
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

    String activityId = execute(cvngServiceClient.registerActivity(context.getAccountId(), activityDTO)).getResource();
    CVNGStateExecutionData cvngStateExecutionData = CVNGStateExecutionData.builder().activityId(activityId).build();
    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(cvngStateExecutionData.getCorrelationId()))
        .executionStatus(ExecutionStatus.RUNNING)
        // .errorMessage(responseMessage)
        .stateExecutionData(cvngStateExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  // TODO: We are just calling one API now. possibly need to move requestExecutor to common module.
  public <U> U execute(Call<U> request) {
    try {
      Response<U> response = request.clone().execute();
      if (response.isSuccessful()) {
        return response.body();
      } else {
        String errorBody = response.errorBody().string();
        throw new IllegalStateException(
            "Code: " + response.code() + ", message: " + response.message() + ", body: " + errorBody);
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Value
  @Builder
  public static class CVNGStateExecutionData extends StateExecutionData {
    private String activityId;
    private String correlationId;
  }
}
