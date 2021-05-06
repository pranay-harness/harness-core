package io.harness.pms.sdk.core.execution;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.tasks.ProgressData;
import io.harness.waiter.ProgressCallback;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Builder
@OwnedBy(PIPELINE)
@Slf4j
public class AsyncSdkProgressCallback implements ProgressCallback {
  @Inject ExecutableProcessorFactory executableProcessorFactory;

  byte[] nodeExecutionBytes;

  @Override
  public void notify(String correlationId, ProgressData progressData) {
    try {
      NodeExecutionProto nodeExecutionProto = NodeExecutionProto.parseFrom(nodeExecutionBytes);
      ExecutableProcessor executableProcessor =
          executableProcessorFactory.obtainProcessor(nodeExecutionProto.getMode());
      executableProcessor.handleProgress(
          ProgressPackage.builder().nodeExecution(nodeExecutionProto).progressData(progressData).build());
    } catch (InvalidProtocolBufferException e) {
      log.error("Not able to deserialize Node Execution from bytes. Progress Callback will not be executed");
    }
  }
}
