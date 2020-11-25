package io.harness.batch.processing.tasklet;

import io.harness.batch.processing.billing.writer.support.ClusterDataGenerationValidator;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.service.intfc.InstanceDataBulkWriteService;
import io.harness.batch.processing.tasklet.reader.PublishedMessageReader;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.PodEvent;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class K8sPodEventTasklet implements Tasklet {
  private JobParameters parameters;
  @Autowired private BatchMainConfig config;
  @Autowired private PublishedMessageDao publishedMessageDao;
  @Autowired private ClusterDataGenerationValidator clusterDataGenerationValidator;
  @Autowired private InstanceDataBulkWriteService instanceDataBulkWriteService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();

    String messageType = EventTypeConstants.K8S_POD_EVENT;
    List<PublishedMessage> publishedMessageList;
    PublishedMessageReader publishedMessageReader =
        new PublishedMessageReader(publishedMessageDao, accountId, messageType, startTime, endTime, batchSize);
    do {
      publishedMessageList = publishedMessageReader.getNext();

      List<InstanceEvent> instanceEventList = publishedMessageList.stream()
                                                  .map(this::processPodEventMessage)
                                                  .filter(instanceEvent -> null != instanceEvent.getAccountId())
                                                  .collect(Collectors.toList());
      instanceDataBulkWriteService.updateList(instanceEventList);
    } while (publishedMessageList.size() == batchSize);
    return null;
  }

  public InstanceEvent processPodEventMessage(PublishedMessage publishedMessage) {
    try {
      return process(publishedMessage);
    } catch (Exception ex) {
      log.error("K8sPodEventTasklet Exception ", ex);
    }
    return InstanceEvent.builder().build();
  }

  public InstanceEvent process(PublishedMessage publishedMessage) {
    PodEvent podEvent = (PodEvent) publishedMessage.getMessage();
    String accountId = publishedMessage.getAccountId();
    String clusterId = podEvent.getClusterId();
    if (!clusterDataGenerationValidator.shouldGenerateClusterData(accountId, clusterId)) {
      return InstanceEvent.builder().build();
    }
    EventType type;
    switch (podEvent.getType()) {
      case EVENT_TYPE_TERMINATED:
        type = EventType.STOP;
        break;
      case EVENT_TYPE_SCHEDULED:
        type = EventType.START;
        break;
      default:
        type = EventType.UNKNOWN;
        break;
    }

    return InstanceEvent.builder()
        .accountId(accountId)
        .cloudProviderId(podEvent.getCloudProviderId())
        .clusterId(clusterId)
        .instanceId(podEvent.getPodUid())
        .type(type)
        .timestamp(HTimestamps.toInstant(podEvent.getTimestamp()))
        .build();
  }
}
