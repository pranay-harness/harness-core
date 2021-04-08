package io.harness.delegate.task.citasks.cik8handler;

import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.CIK8ExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.task.citasks.CIExecuteStepTaskHandler;
import io.harness.logging.CommandExecutionStatus;
import io.harness.product.ci.engine.proto.ExecuteStepRequest;
import io.harness.product.ci.engine.proto.LiteEngineGrpc;

import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CIK8ExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @NotNull private Type type = Type.K8;

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
    } catch (InvalidProtocolBufferException e) {
      log.error("Failed to parse serialized step with err: {}", e.getMessage());
      return K8sTaskExecutionResponse.builder()
          .errorMessage(e.getMessage())
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .build();
    }

    String target = String.format("%s:%d", cik8ExecuteStepTaskParams.getIp(), cik8ExecuteStepTaskParams.getPort());
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
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
}
