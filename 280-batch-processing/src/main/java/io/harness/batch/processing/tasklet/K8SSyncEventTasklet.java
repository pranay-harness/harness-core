package io.harness.batch.processing.tasklet;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.tasklet.reader.PublishedMessageReader;
import io.harness.batch.processing.writer.EventWriter;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.Lifecycle;
import io.harness.event.payloads.Lifecycle.EventType;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.K8SClusterSyncEvent;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.protobuf.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class K8SSyncEventTasklet extends EventWriter implements Tasklet {
  private JobParameters parameters;
  @Autowired private BatchMainConfig config;
  @Autowired private PublishedMessageDao publishedMessageDao;
  @Autowired protected InstanceDataDao instanceDataDao;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    if (config.getBatchQueryConfig().isSyncJobDisabled()) {
      return null;
    }
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    String messageType = EventTypeConstants.K8S_SYNC_EVENT;
    PublishedMessageReader publishedMessageReader =
        new PublishedMessageReader(publishedMessageDao, accountId, messageType, startTime, endTime, batchSize);
    List<PublishedMessage> publishedMessageList;
    do {
      publishedMessageList = publishedMessageReader.getNext();
      processK8SSyncEventMessage(publishedMessageList);
    } while (publishedMessageList.size() == batchSize);
    return null;
  }

  private void processK8SSyncEventMessage(List<PublishedMessage> publishedMessages) {
    try {
      process(publishedMessages);
    } catch (Exception ex) {
      log.error("K8sNodeEventTasklet Exception ", ex);
    }
  }

  public void process(List<PublishedMessage> publishedMessages) {
    log.info("Published batch size is K8SSyncEventTasklet {} ", publishedMessages.size());
    publishedMessages.forEach(publishedMessage -> {
      K8SClusterSyncEvent k8SClusterSyncEvent = (K8SClusterSyncEvent) publishedMessage.getMessage();
      log.info("K8S sync event {} ", k8SClusterSyncEvent);
      String accountId = publishedMessage.getAccountId();
      String clusterId = k8SClusterSyncEvent.getClusterId();
      Timestamp lastProcessedTimestamp = k8SClusterSyncEvent.getLastProcessedTimestamp();
      Set<String> activeInstanceIds =
          fetchActiveInstanceAtTime(accountId, clusterId, HTimestamps.toInstant(lastProcessedTimestamp));
      log.info("Active K8S instances before {} time {}", lastProcessedTimestamp, activeInstanceIds.size());

      Set<String> activeInstanceArns = new HashSet<>();
      activeInstanceArns.addAll(k8SClusterSyncEvent.getActiveNodeUidsList());
      activeInstanceArns.addAll(k8SClusterSyncEvent.getActivePodUidsList());
      activeInstanceArns.addAll(k8SClusterSyncEvent.getActivePvUidsList());

      SetView<String> inactiveInstanceArns = Sets.difference(activeInstanceIds, activeInstanceArns);
      log.info("Inactive K8S instance arns {}", inactiveInstanceArns.toString());

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
