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
import io.harness.product.ci.engine.proto.LiteEngineGrpc;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.GrpcUtil;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIK8ExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @NotNull private Type type = Type.K8;
  public static final String DELEGATE_NAMESPACE = "DELEGATE_NAMESPACE";

  @Override
  public Type getType() {
    return type;
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

    String target = format("%s:%d", cik8ExecuteStepTaskParams.getIp(), cik8ExecuteStepTaskParams.getPort());
    ManagedChannelBuilder managedChannelBuilder = ManagedChannelBuilder.forTarget(target).usePlaintext();
    if (!cik8ExecuteStepTaskParams.isLocal()) {
      managedChannelBuilder.proxyDetector(GrpcUtil.NOOP_PROXY_DETECTOR);
    }
    ManagedChannel channel = managedChannelBuilder.build();
    try {
      try {
        LiteEngineGrpc.LiteEngineBlockingStub liteEngineBlockingStub = LiteEngineGrpc.newBlockingStub(channel);
        liteEngineBlockingStub.withDeadlineAfter(30, TimeUnit.SECONDS).executeStep(executeStepRequest);
        return K8sTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
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
}
