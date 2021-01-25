package io.harness.batch.processing.writer;

import static io.harness.ccm.commons.beans.InstanceType.K8S_PVC;

import io.harness.batch.processing.billing.timeseries.data.K8sGranularUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.tasklet.util.K8sResourceUtils;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.AggregatedStorage;
import io.harness.event.payloads.PVMetric;

import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Singleton
public class PVUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) throws Exception {
    log.info("Published batch size is PVUtilizationMetricsWriter {} ", publishedMessages.size());

    List<K8sGranularUtilizationData> k8sGranularUtilizationDataList =
        publishedMessages.stream()
            .map(publishedMessage -> {
              String accountId = publishedMessage.getAccountId();
              PVMetric pvMetric = (PVMetric) publishedMessage.getMessage();
              // change to debug
              log.debug("PV Utilization {} ", pvMetric);

              AggregatedStorage aggregatedStorage = pvMetric.getAggregatedStorage();
              long endTime = pvMetric.getTimestamp().getSeconds() * 1000;
              long startTime = endTime - (pvMetric.getWindow().getSeconds() * 1000);

              double avgStorageUsageValue = K8sResourceUtils.getMemoryMb(aggregatedStorage.getAvgUsedByte());
              // The capacity of the PVC is actually the Storage Resource requested from the PV. From here it will be
              // called storageResource "Requested".
              double avgStorageRequestValue = K8sResourceUtils.getMemoryMb(aggregatedStorage.getAvgCapacityByte());

              // use this when assocaition with pod is required.
              // String pod_uid = pvMetric.getPodUid();

              return K8sGranularUtilizationData.builder()
                  .accountId(accountId)
                  .instanceId(pvMetric.getName())
                  .instanceType(K8S_PVC.name())
                  .clusterId(pvMetric.getClusterId())
                  .settingId(pvMetric.getCloudProviderId())
                  .storageRequestValue(avgStorageRequestValue)
                  .storageUsageValue(avgStorageUsageValue)
                  .startTimestamp(startTime)
                  .endTimestamp(endTime)
                  .build();
            })
            .collect(Collectors.toList());

    k8sUtilizationGranularDataService.create(k8sGranularUtilizationDataList);
  }
}
