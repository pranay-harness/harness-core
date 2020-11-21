package io.harness.app.resources.sample;

import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.delegate.task.stepstatus.StepStatusTaskParameters;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.tasks.ResponseData;
import io.harness.waiter.NotifyCallback;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.Map;
import java.util.function.Supplier;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

@Api("delegate2-tasks")
@Path("/sample")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class CIDelegateTaskSampleResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";
  public static final String TASK_TYPE = "CI_LE_STATUS";

  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final WaitNotifyEngine waitNotifyEngine;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @POST
  @Path("/parked/http")
  @ApiOperation(value = "Parked task using Delegate 2.0 framework", nickname = "parkedHttp")
  public String parkedHttpTask(@QueryParam("accountId") @NotBlank String accountId,
      @QueryParam("orgIdentifier") @NotBlank String orgIdentifier,
      @QueryParam("projectIdentifier") @NotBlank String projectIdentifier) {
    DelegateCallbackToken delegateCallbackToken = delegateCallbackTokenSupplier.get();

    final int timeoutInSecs = 30;
    final HttpTaskParameters taskParameters = HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build();

    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .parked(true)
                                                        .accountId(accountId)
                                                        .taskType("HTTP")
                                                        .taskParameters(taskParameters)
                                                        .executionTimeout(java.time.Duration.ofSeconds(timeoutInSecs))
                                                        .taskSetupAbstraction("orgIdentifier", orgIdentifier)
                                                        .taskSetupAbstraction("projectIdentifier", projectIdentifier)
                                                        .build();

    String taskId = delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackToken);
    waitNotifyEngine.waitForAllOn(ORCHESTRATION, new SampleNotifyCallback(), taskId);
    return String.format("{\"accountId\": \"%s\", \"taskId\": \"%s\", \"token\": \"%s\"}", accountId, taskId,
        delegateCallbackToken.getToken());
  }

  @POST
  @Path("/async/output")
  @ApiOperation(value = "Create a delegate tasks", nickname = "asyncTaskOutput")
  public String createAsyncStepOutputTask(@QueryParam("accountId") @NotBlank String accountId,
      @QueryParam("orgIdentifier") @NotBlank String orgIdentifier,
      @QueryParam("projectIdentifier") @NotBlank String projectIdentifier) {
    final StepStatusTaskParameters taskParameters = StepStatusTaskParameters.builder().build();

    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .parked(true)
                                                        .accountId(accountId)
                                                        .taskType(TASK_TYPE)
                                                        .taskParameters(taskParameters)
                                                        .executionTimeout(java.time.Duration.ofSeconds(20))
                                                        .taskSetupAbstraction("orgIdentifier", orgIdentifier)
                                                        .taskSetupAbstraction("projectIdentifier", projectIdentifier)
                                                        .build();
    DelegateCallbackToken delegateCallbackToken = delegateCallbackTokenSupplier.get();
    final String taskId = delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackToken);

    waitNotifyEngine.waitForAllOn(ORCHESTRATION, new SampleNotifyCallback(), taskId);
    return String.format("{\"accountId\": \"%s\", \"taskId\": \"%s\", \"token\": \"%s\"}", accountId, taskId,
        delegateCallbackToken.getToken());
  }

  public static class SampleNotifyCallback implements NotifyCallback {
    @Override
    public void notify(Map<String, ResponseData> response) {
      log.info("received response = [{}]", response);
    }

    @Override
    public void notifyError(Map<String, ResponseData> response) {
      log.error("error : [{}]", response);
    }
  }
}
