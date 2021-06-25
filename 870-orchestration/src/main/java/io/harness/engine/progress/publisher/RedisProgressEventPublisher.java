package io.harness.engine.progress.publisher;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.pms.commons.events.PmsEventSender;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.progress.ProgressEvent;
import io.harness.pms.events.base.PmsEventCategory;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.tasks.BinaryResponseData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class RedisProgressEventPublisher implements ProgressEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private PmsEventSender eventSender;

  @Override
  public String publishEvent(String nodeExecutionId, BinaryResponseData progressData) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    String serviceName = nodeExecution.getNode().getServiceName();
    String accountId = AmbianceUtils.getAccountId(nodeExecution.getAmbiance());
    ProgressEvent progressEvent = ProgressEvent.newBuilder()
                                      .setAmbiance(nodeExecution.getAmbiance())
                                      .setExecutionMode(nodeExecution.getMode())
                                      .setStepParameters(nodeExecution.getResolvedStepParametersBytes())
                                      .setProgressBytes(ByteString.copyFrom(progressData.getData()))
                                      .build();

    return eventSender.sendEvent(
        progressEvent.toByteString(), PmsEventCategory.PROGRESS_EVENT, serviceName, accountId, false);
  }
}
