package io.harness.batch.processing.writer;

import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsSyncEvent;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.grpc.utils.HTimestamps;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

@Slf4j
@Singleton
public class EcsSyncEventWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    log.info("Published batch size is EcsSyncEventWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_SYNC_EVENT))
        .forEach(publishedMessage -> {
          EcsSyncEvent ecsSyncEvent = (EcsSyncEvent) publishedMessage.getMessage();
          log.debug("ECS sync event {} ", ecsSyncEvent);
          String accountId = publishedMessage.getAccountId();
          String clusterId = ecsSyncEvent.getClusterId();
          Timestamp lastProcessedTimestamp = ecsSyncEvent.getLastProcessedTimestamp();
          Set<String> activeInstanceIds =
              fetchActiveInstanceAtTime(accountId, clusterId, HTimestamps.toInstant(lastProcessedTimestamp));
          log.debug("Active instances before {} time {}", lastProcessedTimestamp, activeInstanceIds);

          Set<String> activeInstanceArns = new HashSet<>();
          activeInstanceArns.addAll(ecsSyncEvent.getActiveEc2InstanceArnsList());
          activeInstanceArns.addAll(
              ecsSyncEvent.getActiveTaskArnsList().stream().map(this::getIdFromArn).collect(Collectors.toList()));
          activeInstanceArns.addAll(ecsSyncEvent.getActiveContainerInstanceArnsList()
                                        .stream()
                                        .map(this::getIdFromArn)
                                        .collect(Collectors.toList()));
          SetView<String> inactiveInstanceArns = Sets.difference(activeInstanceIds, activeInstanceArns);
          log.info("Inactive instance arns {}", inactiveInstanceArns.toString());

          inactiveInstanceArns.forEach(inactiveInstanceArn
              -> handleLifecycleEvent(
                  accountId, createLifecycle(inactiveInstanceArn, clusterId, lastProcessedTimestamp)));
        });
  }

  private Lifecycle createLifecycle(String instanceId, String clusterId, Timestamp lastProcessedTimestamp) {
    return Lifecycle.newBuilder()
        .setInstanceId(instanceId)
        .setClusterId(clusterId)
        .setType(EventType.EVENT_TYPE_STOP)
        .setTimestamp(lastProcessedTimestamp)
        .build();
  }
}
