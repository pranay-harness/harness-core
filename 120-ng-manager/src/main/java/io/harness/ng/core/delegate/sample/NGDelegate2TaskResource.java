package io.harness.ng.core.delegate.sample;

import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import com.google.inject.Inject;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.waiter.WaitNotifyEngine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

import java.util.function.Supplier;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Path("/delegate2-tasks")
@Api("/delegate2-tasks")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class NGDelegate2TaskResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";

  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final KryoSerializer kryoSerializer;
  private final DelegateSyncService delegateSyncService;
  private final WaitNotifyEngine waitNotifyEngine;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

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
    return delegateServiceGrpcClient.executeSyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get());
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
    final String taskId =
        delegateServiceGrpcClient.submitAsyncTask(delegateTaskRequest, delegateCallbackTokenSupplier.get());

    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION, new SimpleNotifyCallback(), taskId);
    return taskId;
  }
}
