package io.harness.batch.processing.tasklet;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.InstanceEvent;
import io.harness.batch.processing.ccm.InstanceEvent.EventType;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.dao.intfc.PublishedMessageDao;
import io.harness.batch.processing.service.intfc.InstanceDataBulkWriteService;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.tasklet.reader.PublishedMessageReader;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.grpc.utils.HTimestamps;
import io.harness.perpetualtask.k8s.watch.NodeEvent;

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
public class K8sNodeEventTasklet implements Tasklet {
  private JobParameters parameters;
  @Autowired private BatchMainConfig config;
  @Autowired private PublishedMessageDao publishedMessageDao;
  @Autowired protected InstanceDataService instanceDataService;
  @Autowired private InstanceDataBulkWriteService instanceDataBulkWriteService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    Long startTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_START_DATE);
    Long endTime = CCMJobConstants.getFieldLongValueFromJobParams(parameters, CCMJobConstants.JOB_END_DATE);
    int batchSize = config.getBatchQueryConfig().getQueryBatchSize();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);

    String messageType = EventTypeConstants.K8S_NODE_EVENT;
    PublishedMessageReader publishedMessageReader =
        new PublishedMessageReader(publishedMessageDao, accountId, messageType, startTime, endTime, batchSize);
    List<PublishedMessage> publishedMessageList;
    do {
      publishedMessageList = publishedMessageReader.getNext();
      List<InstanceEvent> instanceEventList = publishedMessageList.stream()
                                                  .map(this::processNodeEventMessage)
                                                  .filter(instanceInfo -> null != instanceInfo.getAccountId())
                                                  .collect(Collectors.toList());

      instanceDataBulkWriteService.updateList(instanceEventList);
    } while (publishedMessageList.size() == batchSize);
    return null;
  }

  public InstanceEvent processNodeEventMessage(PublishedMessage publishedMessage) {
    try {
      return process(publishedMessage);
    } catch (Exception ex) {
      log.error("K8sNodeEventTasklet Exception ", ex);
    }
    return InstanceEvent.builder().build();
  }

  public InstanceEvent process(PublishedMessage publishedMessage) {
    NodeEvent nodeEvent = (NodeEvent) publishedMessage.getMessage();
    EventType type;
    switch (nodeEvent.getType()) {
      case EVENT_TYPE_START:
        type = EventType.START;
        break;
      case EVENT_TYPE_STOP:
        type = EventType.STOP;
        break;
      default:
        type = EventType.UNKNOWN;
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
