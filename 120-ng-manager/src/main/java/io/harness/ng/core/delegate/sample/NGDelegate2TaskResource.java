package io.harness.ng.core.delegate.sample;

import static io.harness.waiter.NgOrchestrationNotifyEventListener.NG_ORCHESTRATION;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.Duration;

import io.harness.callback.DelegateCallbackToken;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.AccountId;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskId;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.beans.ResponseData;
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
  public ResponseData createSyncTaskD2(@QueryParam("accountId") @NotBlank String accountId) {
    final HttpTaskParameters taskParameters = HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build();
    final int timeoutInSecs = 30;
    final TaskId taskId = delegateServiceGrpcClient.submitTask(delegateCallbackTokenSupplier.get(),
        AccountId.newBuilder().setId(accountId).build(),
        TaskSetupAbstractions.newBuilder().putValues("accountId", accountId).build(),
        TaskDetails.newBuilder()
            .setMode(TaskMode.SYNC)
            .setType(TaskType.newBuilder().setType("HTTP").build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters)))
            .setExecutionTimeout(Duration.newBuilder().setSeconds(timeoutInSecs).setNanos(0).build())
            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
            .build(),
        taskParameters.fetchRequiredExecutionCapabilities());
    logger.info("sync task id =[{}]", taskId.getId());
    return delegateSyncService.waitForTask(taskId.getId(), "", java.time.Duration.ofSeconds(timeoutInSecs));
  }

  @POST
  @Path("async")
  @ApiOperation(value = "Create a delegate tasks", nickname = "asyncTaskD2")
  public String createAsyncTaskD2(@QueryParam("accountId") @NotBlank String accountId) {
    final HttpTaskParameters taskParameters = HttpTaskParameters.builder().method("GET").url(HTTP_URL_200).build();
    final TaskId taskId = delegateServiceGrpcClient.submitTask(delegateCallbackTokenSupplier.get(),
        AccountId.newBuilder().setId(accountId).build(),
        TaskSetupAbstractions.newBuilder().putValues("accountId", accountId).build(),
        TaskDetails.newBuilder()
            .setMode(TaskMode.ASYNC)
            .setType(TaskType.newBuilder().setType("HTTP").build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters)))
            .setExecutionTimeout(Duration.newBuilder().setSeconds(20).setNanos(0).build())
            .setExpressionFunctorToken(HashGenerator.generateIntegerHash())
            .build(),
        taskParameters.fetchRequiredExecutionCapabilities());
    waitNotifyEngine.waitForAllOn(NG_ORCHESTRATION, new SimpleNotifyCallback(), taskId.getId());
    return taskId.getId();
  }
}
