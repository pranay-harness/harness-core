package io.harness.batch.processing.writer;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.data.PrunedInstanceData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class K8sUtilizationMetricsWriter extends EventWriter implements ItemWriter<List<String>> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  private static final int BATCH_SIZE = 50;
  private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
  }

  @Override
  public void write(List<? extends List<String>> lists) {
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    long startDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_START_DATE));
    long endDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_END_DATE));
    logger.info("Published batch size is K8sUtilizationMetricsWriter {} ", lists.size());
    List<InstanceUtilizationData> instanceUtilizationDataList = new ArrayList<>();

    lists.forEach(list -> Lists.partition(list, BATCH_SIZE).forEach(instanceIdsBatch -> {
      Map<String, InstanceUtilizationData> aggregatedUtilizationData =
          k8sUtilizationGranularDataService.getAggregatedUtilizationData(
              accountId, instanceIdsBatch, startDate, endDate);

      for (Map.Entry<String, InstanceUtilizationData> entry : aggregatedUtilizationData.entrySet()) {
        InstanceUtilizationData instanceUtilizationData = entry.getValue();
        String settingId = instanceUtilizationData.getSettingId();
        String instanceId = entry.getKey();

        PrunedInstanceData instanceData =
            instanceDataService.fetchPrunedInstanceDataWithName(accountId, settingId, instanceId, startDate);
        // Initialisation to 100% Utilisation

        if (instanceData.getInstanceId() != null) {
          double cpuAvgPercentage = 1;
          double cpuMaxPercentage = 1;
          double memoryAvgPercentage = 1;
          double memoryMaxPercentage = 1;

          double cpuAvgValue = instanceUtilizationData.getCpuUtilizationAvg();
          double cpuMaxValue = instanceUtilizationData.getCpuUtilizationMax();
          double memoryAvgValue = instanceUtilizationData.getMemoryUtilizationAvg();
          double memoryMaxValue = instanceUtilizationData.getMemoryUtilizationMax();
          if (instanceData.getTotalResource() != null) {
            Double totalCpuResource = instanceData.getTotalResource().getCpuUnits();
            Double totalMemoryResource = instanceData.getTotalResource().getMemoryMb();
            if (totalCpuResource != 0) {
              cpuAvgPercentage = cpuAvgValue / totalCpuResource;
              cpuMaxPercentage = cpuMaxValue / totalCpuResource;
            }
            if (totalMemoryResource != 0) {
              memoryAvgPercentage = memoryAvgValue / totalMemoryResource;
              memoryMaxPercentage = memoryMaxValue / totalMemoryResource;
            }
          }
          instanceUtilizationData.setInstanceId(instanceData.getInstanceId());
          instanceUtilizationData.setCpuUtilizationAvg(cpuAvgPercentage);
          instanceUtilizationData.setCpuUtilizationMax(cpuMaxPercentage);
          instanceUtilizationData.setMemoryUtilizationAvg(memoryAvgPercentage);
          instanceUtilizationData.setMemoryUtilizationMax(memoryMaxPercentage);
          instanceUtilizationData.setCpuUtilizationAvgValue(cpuAvgValue);
          instanceUtilizationData.setCpuUtilizationMaxValue(cpuMaxValue);
          instanceUtilizationData.setMemoryUtilizationAvgValue(memoryAvgValue);
          instanceUtilizationData.setMemoryUtilizationMaxValue(memoryMaxValue);
          instanceUtilizationData.setStartTimestamp(startDate);
          instanceUtilizationData.setEndTimestamp(endDate);

          instanceUtilizationDataList.add(instanceUtilizationData);
        }
      }
    }));
    utilizationDataService.create(instanceUtilizationDataList);
  }
}
