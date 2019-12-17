package io.harness.batch.processing.reader;

import io.harness.batch.processing.billing.timeseries.service.impl.K8sUtilizationGranularDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class K8sGranularUtilizationMetricsReader implements ItemReader<List<String>> {
  @Autowired private K8sUtilizationGranularDataServiceImpl k8sUtilizationGranularDataService;

  private AtomicBoolean runOnlyOnce;
  private JobParameters parameters;

  @BeforeStep
  public void beforeStep(final StepExecution stepExecution) {
    parameters = stepExecution.getJobExecution().getJobParameters();
    runOnlyOnce = new AtomicBoolean(false);
  }

  @Override
  public List<String> read() {
    List<String> distinctInstanceIds = null;
    if (!runOnlyOnce.getAndSet(true)) {
      long startDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_START_DATE));
      long endDate = Long.parseLong(parameters.getString(CCMJobConstants.JOB_END_DATE));
      distinctInstanceIds = k8sUtilizationGranularDataService.getDistinctInstantIds(startDate, endDate);
    }
    return distinctInstanceIds;
  }
}
