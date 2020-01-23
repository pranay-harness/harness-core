package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.K8S_POD;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.processor.util.K8sResourceUtils;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.PodMetric;
import io.harness.event.payloads.PodMetric.Container;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class PodUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) {
    logger.info("Published batch size is PodUtilizationMetricsWriter {} ", publishedMessages.size());
    List<K8sGranularUtilizationData> k8sGranularUtilizationDataList = new ArrayList<>();
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.POD_UTILIZATION))
        .forEach(publishedMessage -> {
          String accountId = publishedMessage.getAccountId();
          PodMetric podUtilizationMetric = (PodMetric) publishedMessage.getMessage();
          logger.debug("Pod Utilization {} ", podUtilizationMetric);

          long endTime = podUtilizationMetric.getTimestamp().getSeconds() * 1000;
          long startTime = endTime - (podUtilizationMetric.getWindow().getSeconds() * 1000);

          double cpuUnits = 0.0;
          double memoryMb = 0.0;

          for (Container container : podUtilizationMetric.getContainersList()) {
            cpuUnits += K8sResourceUtils.getCpuUnits(container.getUsage().getCpuNano());
            memoryMb += K8sResourceUtils.getMemoryMb(container.getUsage().getMemoryByte());
          }

          K8sGranularUtilizationData k8sGranularUtilizationData =
              K8sGranularUtilizationData.builder()
                  .accountId(accountId)
                  .instanceId(podUtilizationMetric.getName())
                  .instanceType(K8S_POD)
                  .clusterId(podUtilizationMetric.getClusterId())
                  .settingId(podUtilizationMetric.getCloudProviderId())
                  .startTimestamp(startTime)
                  .endTimestamp(endTime)
                  .cpu(cpuUnits)
                  .memory(memoryMb)
                  .build();

          k8sGranularUtilizationDataList.add(k8sGranularUtilizationData);
        });
    k8sUtilizationGranularDataService.create(k8sGranularUtilizationDataList);
  }
}
