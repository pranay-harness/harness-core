package io.harness.engine.progress.publisher;

import static io.harness.pms.events.PmsEventFrameworkConstants.SERVICE_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.utils.OrchestrationEventsFrameworkUtils;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.progress.ProgressEvent;
import io.harness.tasks.BinaryResponseData;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class RedisProgressEventPublisher implements ProgressEventPublisher {
  @Inject private NodeExecutionService nodeExecutionService;
  @Inject private OrchestrationEventsFrameworkUtils eventsFrameworkUtils;

  @Override
  public String publishEvent(String nodeExecutionId, BinaryResponseData progressData) {
    NodeExecution nodeExecution = nodeExecutionService.get(nodeExecutionId);
    ProgressEvent progressEvent = ProgressEvent.newBuilder()
                                      .setAmbiance(nodeExecution.getAmbiance())
                                      .setExecutionMode(nodeExecution.getMode())
                                      .setStepParameters(nodeExecution.getNode().getStepParametersBytes())
                                      .setProgressBytes(ByteString.copyFrom(progressData.getData()))
                                      .build();

    Producer producer = eventsFrameworkUtils.obtainProducerForProgressEvent(nodeExecution.getNode().getServiceName());

    producer.send(Message.newBuilder()
                      .putAllMetadata(ImmutableMap.of(SERVICE_NAME, nodeExecution.getNode().getServiceName()))
                      .setData(progressEvent.toByteString())
                      .build());
    return null;
  }
}
