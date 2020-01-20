package io.harness.batch.processing.writer;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;

import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8SClusterSyncEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Singleton
public class K8SSyncEventWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    logger.info("Published batch size is K8SSyncEventWriter {} ", publishedMessages.size());
    publishedMessages.forEach(publishedMessage -> {
      K8SClusterSyncEvent k8SClusterSyncEvent = (K8SClusterSyncEvent) publishedMessage.getMessage();
      logger.info("K8S sync event {} ", k8SClusterSyncEvent);
      String accountId = publishedMessage.getAccountId();
      String clusterId = k8SClusterSyncEvent.getClusterId();
      Timestamp lastProcessedTimestamp = k8SClusterSyncEvent.getLastProcessedTimestamp();
      Set<String> activeInstanceIds =
          fetchActiveInstanceAtTime(accountId, clusterId, HTimestamps.toInstant(lastProcessedTimestamp));
      logger.debug("Active K8S instances before {} time {}", lastProcessedTimestamp, activeInstanceIds);

      Set<String> activeInstanceArns = new HashSet<>();
      activeInstanceArns.addAll(k8SClusterSyncEvent.getActiveNodeUidsList());
      activeInstanceArns.addAll(k8SClusterSyncEvent.getActivePodUidsList());
      SetView<String> inactiveInstanceArns = Sets.difference(activeInstanceIds, activeInstanceArns);
      logger.info("Inactive K8S instance arns {}", inactiveInstanceArns.toString());

      inactiveInstanceArns.forEach(inactiveInstanceArn
          -> handleLifecycleEvent(accountId, createLifecycle(inactiveInstanceArn, clusterId, lastProcessedTimestamp)));
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