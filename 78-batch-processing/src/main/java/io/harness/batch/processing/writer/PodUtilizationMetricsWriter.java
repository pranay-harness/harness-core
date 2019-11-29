package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.K8S_POD;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.PodMetric;
import io.harness.event.payloads.PodMetric.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
@Singleton
public class PodUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) {
    logger.info("Published batch size is PodUtilizationMetricsWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.POD_UTILIZATION))
        .forEach(publishedMessage -> {
          PodMetric podUtilizationMetric = (PodMetric) publishedMessage.getMessage();
          logger.info("Pod Utilization {} ", podUtilizationMetric);

          Long endTime = podUtilizationMetric.getTimestamp().getSeconds() * 1000;
          Long startTime = endTime - (podUtilizationMetric.getWindow().getSeconds() * 1000);

          Double cpuUsageWithUnits = 0.0;
          Double memoryUsageWithUnits = 0.0;

          for (Container container : podUtilizationMetric.getContainersList()) {
            // TODO: (Rohit) Remove this typecast once fixed on the PT end
            cpuUsageWithUnits += Double.valueOf(container.getUsage().getCpu());
            memoryUsageWithUnits += Double.valueOf(container.getUsage().getMemory());
          }

          K8sGranularUtilizationData k8sGranularUtilizationData =
              K8sGranularUtilizationData.builder()
                  .instanceId(podUtilizationMetric.getName())
                  .instanceType(K8S_POD)
                  .settingId(podUtilizationMetric.getCloudProviderId())
                  .startTimestamp(startTime)
                  .endTimestamp(endTime)
                  .cpu(cpuUsageWithUnits)
                  .memory(memoryUsageWithUnits)
                  .build();

          k8sUtilizationGranularDataService.create(k8sGranularUtilizationData);
        });
  }
}
