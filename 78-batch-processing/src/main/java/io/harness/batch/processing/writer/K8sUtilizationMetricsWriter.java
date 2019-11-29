package io.harness.batch.processing.writer;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
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
        String instanceId = entry.getKey();
        InstanceUtilizationData instanceUtilizationData = entry.getValue();
        utilizationDataService.create(instanceUtilizationData);
      }
    }));
  }
}
