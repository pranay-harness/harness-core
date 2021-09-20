package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.CIK8ExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.CIExecuteStepTaskHandler;
import io.harness.exception.InvalidArgumentsException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.product.ci.engine.proto.ExecuteStepRequest;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.RetryPolicy;
import okhttp3.Response;

@Slf4j
public class CIK8ExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @NotNull private Type type = Type.K8;
  public static final String DELEGATE_NAMESPACE = "DELEGATE_NAMESPACE";
  private final Duration RETRY_SLEEP_DURATION = Duration.ofSeconds(2);
  private final int MAX_ATTEMPTS = 3;
  @Inject
  private HttpHelper httpHelper;

  @Override
  public Type getType() {
    return type;
  }

  public K8sTaskExecutionResponse callDrone(ExecuteStepRequest request, CIK8ExecuteStepTaskParams taskParams) {
    if (!request.getStep().hasRun()) {
      throw new InvalidArgumentsException("Invalid step type");
    }

    String jsonDump = "";
    try {
      JsonFormat.Printer printer = JsonFormat.printer().includingDefaultValueFields();
      jsonDump = printer.print(request);
    } catch (InvalidProtocolBufferException ex) {
      ex.printStackTrace();
    }

    Map<String, String> params = new HashMap<>();
    params.put("command", request.getStep().getRun().getCommand());
    params.put("image", request.getStep().getRun().getImage());
    params.put("step_id", request.getStep().getId());
    params.put("log_key", request.getStep().getLogKey());
    params.put("dump", jsonDump);
    params.put("stage_id", taskParams.getPodName());

    Response response = httpHelper.call("http://127.0.0.1:9191/execute_step", params);
    if (response == null || !response.isSuccessful()) {

      log.error("Response not Successful. Response body: {}", response);
      return K8sTaskExecutionResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.FAILURE)
              .build();
    } else {
      return K8sTaskExecutionResponse.builder()
              .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
              .build();
    }
  }

  @Override
  public K8sTaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams) {
    CIK8ExecuteStepTaskParams cik8ExecuteStepTaskParams = (CIK8ExecuteStepTaskParams) ciExecuteStepTaskParams;

    ExecuteStepRequest executeStepRequest;
    try {
      executeStepRequest = ExecuteStepRequest.parseFrom(cik8ExecuteStepTaskParams.getSerializedStep());
      log.info("parsed call for execute step with id {} is successful ", executeStepRequest.getStep().getId());
    } catch (InvalidProtocolBufferException e) {
      log.error("Failed to parse serialized step with err: {}", e.getMessage());
      return K8sTaskExecutionResponse.builder()
          .errorMessage(e.getMessage())
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .build();
    }

    String namespacedDelegateSvcEndpoint =
        getNamespacedDelegateSvcEndpoint(cik8ExecuteStepTaskParams.getDelegateSvcEndpoint());
    log.info("Delegate service endpoint for step {}: {}", executeStepRequest.getStep().getId(),
        namespacedDelegateSvcEndpoint);
    if (isNotEmpty(namespacedDelegateSvcEndpoint)) {
      executeStepRequest = executeStepRequest.toBuilder().setDelegateSvcEndpoint(namespacedDelegateSvcEndpoint).build();
    }

    final ExecuteStepRequest finalExecuteStepRequest = executeStepRequest;
    return callDrone(executeStepRequest, cik8ExecuteStepTaskParams);

    /*
    String target = format("%s:%d", cik8ExecuteStepTaskParams.getIp(), cik8ExecuteStepTaskParams.getPort());
    ManagedChannelBuilder managedChannelBuilder = ManagedChannelBuilder.forTarget(target).usePlaintext();
    if (!cik8ExecuteStepTaskParams.isLocal()) {
      managedChannelBuilder.proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR);
    }
    ManagedChannel channel = managedChannelBuilder.build();
    try {
      try {
        RetryPolicy<Object> retryPolicy =
            getRetryPolicy(format("[Retrying failed call to send execution call to pod %s: {}",
                               ((CIK8ExecuteStepTaskParams) ciExecuteStepTaskParams).getIp()),
                format("Failed to send execution to pod %s after retrying {} times",
                    ((CIK8ExecuteStepTaskParams) ciExecuteStepTaskParams).getIp()));

        return Failsafe.with(retryPolicy).get(() -> {
          LiteEngineGrpc.LiteEngineBlockingStub liteEngineBlockingStub = LiteEngineGrpc.newBlockingStub(channel);
          liteEngineBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).executeStep(finalExecuteStepRequest);
          return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
        });

      } finally {
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        channel.shutdownNow();
      }
    } catch (Exception e) {
      log.error("Failed to execute step on lite engine target {} with err: {}", target, e);
      return K8sTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(e.getMessage())
          .build();
    }

     */
  }

  private String getNamespacedDelegateSvcEndpoint(String delegateSvcEndpoint) {
    String namespace = System.getenv(DELEGATE_NAMESPACE);
    if (isEmpty(namespace)) {
      return delegateSvcEndpoint;
    }

    String[] svcArr = delegateSvcEndpoint.split(":");
    if (svcArr.length != 2) {
      throw new InvalidArgumentsException(
          format("Delegate service endpoint provided is invalid: %s", delegateSvcEndpoint));
    }

    return format("%s.%s.svc.cluster.local:%s", svcArr[0], namespace, svcArr[1]);
  }

  private RetryPolicy<Object> getRetryPolicy(String failedAttemptMessage, String failureMessage) {
    return new RetryPolicy<>()
        .handle(Exception.class)
        .withDelay(RETRY_SLEEP_DURATION)
        .withMaxAttempts(MAX_ATTEMPTS)
        .onFailedAttempt(event -> log.info(failedAttemptMessage, event.getAttemptCount(), event.getLastFailure()))
        .onFailure(event -> log.error(failureMessage, event.getAttemptCount(), event.getFailure()));
  }
}
