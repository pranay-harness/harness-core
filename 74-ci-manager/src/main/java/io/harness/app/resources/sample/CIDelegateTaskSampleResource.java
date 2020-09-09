package io.harness.app.resources.sample;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.util.Durations;

import io.harness.beans.DelegateTaskRequest;
import io.harness.callback.DelegateCallbackToken;
import io.harness.delegate.AccountId;
import io.harness.delegate.SubmitTaskResponse;
import io.harness.delegate.TaskDetails;
import io.harness.delegate.TaskMode;
import io.harness.delegate.TaskSetupAbstractions;
import io.harness.delegate.TaskType;
import io.harness.delegate.task.http.HttpTaskParameters;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.serializer.KryoSerializer;
import io.harness.waiter.WaitNotifyEngine;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Collections;
import java.util.function.Supplier;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("delegate2-tasks")
@Path("/sample")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
@Slf4j
public class CIDelegateTaskSampleResource {
  private static final String HTTP_URL_200 = "http://httpstat.us/200";

  private final DelegateServiceGrpcClient delegateServiceGrpcClient;
  private final KryoSerializer kryoSerializer;
  private final WaitNotifyEngine waitNotifyEngine;
  private final Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @POST
  @Path("/parked/http")
  @ApiOperation(value = "Parked task using Delegate 2.0 framework", nickname = "parkedHttp")
  public String submitTask(@QueryParam("accountId") @NotBlank String accountId,
      @QueryParam("orgIdentifier") @NotBlank String orgIdentifier,
      @QueryParam("projectIdentifier") @NotBlank String projectIdentifier) {
    DelegateCallbackToken delegateCallbackToken = delegateCallbackTokenSupplier.get();

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

    final TaskSetupAbstractions taskSetupAbstractions =
        TaskSetupAbstractions.newBuilder()
            .putAllValues(MapUtils.emptyIfNull(delegateTaskRequest.getTaskSetupAbstractions()))
            .build();

    TaskDetails taskDetails =
        TaskDetails.newBuilder()
            .setParked(true)
            .setMode(TaskMode.ASYNC)
            .setType(TaskType.newBuilder().setType(delegateTaskRequest.getTaskType()).build())
            .setKryoParameters(ByteString.copyFrom(kryoSerializer.asDeflatedBytes(taskParameters)))
            .setExecutionTimeout(Durations.fromSeconds(delegateTaskRequest.getExecutionTimeout().getSeconds()))
            .build();

    AccountId acctId = AccountId.newBuilder().setId(accountId).build();
    SubmitTaskResponse submitTaskResponse = delegateServiceGrpcClient.submitTask(delegateCallbackToken, acctId,
        taskSetupAbstractions, taskDetails, Collections.emptyList(), Collections.emptyList());

    String taskId = submitTaskResponse.getTaskId().getId();
    return String.format("{\"accountId\": \"%s\", \"taskId\": \"%s\", \"token\": \"%s\"}", accountId, taskId,
        delegateCallbackToken.getToken());
  }
}
