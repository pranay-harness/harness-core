package io.harness.batch.processing.writer;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.entities.InstanceData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

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
    long startDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_START_DATE));
    long endDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_END_DATE));
    logger.info("Published batch size is K8sUtilizationMetricsWriter {} ", lists.size());
    lists.forEach(list -> Lists.partition(list, BATCH_SIZE).forEach(instanceIdsBatch -> {
      Map<String, InstanceUtilizationData> aggregatedUtilizationData =
          k8sUtilizationGranularDataService.getAggregatedUtilizationData(instanceIdsBatch, startDate, endDate);

      for (Map.Entry<String, InstanceUtilizationData> entry : aggregatedUtilizationData.entrySet()) {
        InstanceUtilizationData instanceUtilizationData = entry.getValue();
        String accountId = instanceUtilizationData.getAccountId();
        String instanceId = entry.getKey();

        InstanceData instanceData = instanceDataService.fetchInstanceDataWithName(accountId, instanceId, startDate);
        // Initialisation to 100% Utilisation
        double cpuAvgPercentage = 1;
        double cpuMaxPercentage = 1;
        double memoryAvgPercentage = 1;
        double memoryMaxPercentage = 1;

        if (null != instanceData && instanceData.getTotalResource() != null) {
          Double totalCpuResource = instanceData.getTotalResource().getCpuUnits();
          Double totalMemoryResource = instanceData.getTotalResource().getMemoryMb();
          if (totalCpuResource != 0) {
            cpuAvgPercentage = instanceUtilizationData.getCpuUtilizationAvg() / totalCpuResource;
            cpuMaxPercentage = instanceUtilizationData.getCpuUtilizationMax() / totalCpuResource;
          }
          if (totalMemoryResource != 0) {
            memoryAvgPercentage = instanceUtilizationData.getMemoryUtilizationAvg() / totalMemoryResource;
            memoryMaxPercentage = instanceUtilizationData.getMemoryUtilizationMax() / totalMemoryResource;
          }
          instanceUtilizationData.setInstanceId(instanceData.getInstanceId());
        }

        instanceUtilizationData.setCpuUtilizationAvg(cpuAvgPercentage);
        instanceUtilizationData.setCpuUtilizationMax(cpuMaxPercentage);
        instanceUtilizationData.setMemoryUtilizationAvg(memoryAvgPercentage);
        instanceUtilizationData.setMemoryUtilizationMax(memoryMaxPercentage);
        instanceUtilizationData.setStartTimestamp(startDate);
        instanceUtilizationData.setEndTimestamp(endDate);

        utilizationDataService.create(instanceUtilizationData);
      }
    }));
  }
}
