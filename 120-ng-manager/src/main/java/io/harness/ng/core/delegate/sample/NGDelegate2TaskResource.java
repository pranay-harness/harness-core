package io.harness.ng.core.delegate.sample;

import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import io.harness.beans.DelegateTaskRequest;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.waiter.WaitNotifyEngine;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

@Path("/delegate2-tasks")
@Api("/delegate2-tasks")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class NGDelegate2TaskResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";

  private final WaitNotifyEngine waitNotifyEngine;
  private final DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @POST
  @Path("sync")
  @ApiOperation(value = "Sync task using Delegate 2.0 framework", nickname = "syncTaskD2")
  public DelegateResponseData createSyncTaskD2(@QueryParam("accountId") @NotBlank String accountId,
      @QueryParam("orgIdentifier") @NotBlank String orgIdentifier,
      @QueryParam("projectIdentifier") @NotBlank String projectIdentifier) {
    final int timeoutInSecs = 30;
    final HttpTaskParameters taskParameters = HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build();
    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountId)
                                                        .taskType("HTTP")
                                                        .taskParameters(taskParameters)
                                                        .executionTimeout(java.time.Duration.ofSeconds(timeoutInSecs))
                                                        .taskSetupAbstraction("orgIdentifier", orgIdentifier)
                                                        .taskSetupAbstraction("projectIdentifier", projectIdentifier)
                                                        .build();
    return delegateGrpcClientWrapper.executeSyncTask(delegateTaskRequest);
  }

  @POST
  @Path("async")
  @ApiOperation(value = "Create a delegate tasks", nickname = "asyncTaskD2")
  public String createAsyncTaskD2(@QueryParam("accountId") @NotBlank String accountId,
      @QueryParam("orgIdentifier") @NotBlank String orgIdentifier,
      @QueryParam("projectIdentifier") @NotBlank String projectIdentifier) {
    final HttpTaskParameters taskParameters = HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build();

    final DelegateTaskRequest delegateTaskRequest = DelegateTaskRequest.builder()
                                                        .accountId(accountId)
                                                        .taskType("HTTP")
                                                        .taskParameters(taskParameters)
                                                        .executionTimeout(java.time.Duration.ofSeconds(20))
                                                        .taskSetupAbstraction("orgIdentifier", orgIdentifier)
                                                        .taskSetupAbstraction("projectIdentifier", projectIdentifier)
                                                        .build();
    final String taskId = delegateGrpcClientWrapper.submitAsyncTask(delegateTaskRequest);

    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION, new SimpleNotifyCallback(), taskId);
    return taskId;
  }
}
