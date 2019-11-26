package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationJobType.K8S_NODE;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.NodeMetric;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@Singleton
public class NodeUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) {
    logger.info("Published batch size is NodeUtilizationMetricsWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.NODE_UTILIZATION))
        .forEach(publishedMessage -> {
          NodeMetric nodeUtilizationMetric = (NodeMetric) publishedMessage.getMessage();
          logger.info("Node Utilization {} ", nodeUtilizationMetric);

          Long endTime = nodeUtilizationMetric.getTimestamp().getSeconds() * 1000;
          Long startTime = endTime - (nodeUtilizationMetric.getWindow().getSeconds() * 1000);
          // TODO: (Rohit) Remove this typecast once fixed on the PT end
          Double cpuUsageWithUnits = Double.valueOf(nodeUtilizationMetric.getUsage().getCpu());
          Double memoryUsageWithUnits = Double.valueOf(nodeUtilizationMetric.getUsage().getMemory());

          K8sGranularUtilizationData k8sGranularUtilizationData =
              K8sGranularUtilizationData.builder()
                  .instanceId(nodeUtilizationMetric.getName())
                  .instanceType(K8S_NODE)
                  .settingId(nodeUtilizationMetric.getCloudProviderId())
                  .startTimestamp(startTime)
                  .endTimestamp(endTime)
                  .cpu(cpuUsageWithUnits)
                  .memory(memoryUsageWithUnits)
                  .build();

          k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
        });
  }
}