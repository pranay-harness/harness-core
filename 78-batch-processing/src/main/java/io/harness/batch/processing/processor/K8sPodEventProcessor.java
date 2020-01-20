package io.harness.batch.processing.processor;

import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.PodEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

@Slf4j
public class K8sPodEventProcessor implements ItemProcessor<PublishedMessage, InstanceEvent> {
  @Override
  public InstanceEvent process(PublishedMessage publishedMessage) throws Exception {
    PodEvent podEvent = (PodEvent) publishedMessage.getMessage(); // TODO: move this logic to reader
    EventType type = null;
    switch (podEvent.getType()) {
      case EVENT_TYPE_DELETED:
        type = EventType.STOP;
        break;
      case EVENT_TYPE_SCHEDULED:
        type = EventType.START;
        break;
      default:
        break;
    }

    return InstanceEvent.builder()
        .accountId(publishedMessage.getAccountId())
        .cloudProviderId(podEvent.getCloudProviderId())
        .clusterId(podEvent.getClusterId())
        .instanceId(podEvent.getPodUid())
        .type(type)
        .timestamp(HTimestamps.toInstant(podEvent.getTimestamp()))
        .build();
  }
}
