package io.harness.batch.processing.processor;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.NodeEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class K8sNodeEventProcessor implements ItemProcessor<PublishedMessage, InstanceEvent> {
  @Override
  public InstanceEvent process(PublishedMessage publishedMessage) throws Exception {
    NodeEvent nodeEvent = (NodeEvent) publishedMessage.getMessage(); // TODO: move this logic to reader
    EventType type = null;
    switch (nodeEvent.getType()) {
      case EVENT_TYPE_START:
        type = EventType.START;
        break;
      case EVENT_TYPE_STOP:
        type = EventType.STOP;
        break;
      default:
        break;
    }

    return InstanceEvent.builder()
        .accountId(publishedMessage.getAccountId())
        .cloudProviderId(nodeEvent.getCloudProviderId())
        .clusterId(nodeEvent.getClusterId())
        .instanceId(nodeEvent.getNodeUid())
        .instanceName(nodeEvent.getNodeName())
        .type(type)
        .timestamp(HTimestamps.toInstant(nodeEvent.getTimestamp()))
        .build();
  }
}
