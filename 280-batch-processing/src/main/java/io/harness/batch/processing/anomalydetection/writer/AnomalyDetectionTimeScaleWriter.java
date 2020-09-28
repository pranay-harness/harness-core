package io.harness.batch.processing.anomalydetection.writer;

import io.harness.batch.processing.anomalydetection.Anomaly;
import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public class AnomalyDetectionTimeScaleWriter implements ItemWriter<Anomaly>, StepExecutionListener {
  @Autowired private AnomalyDetectionTimescaleDataServiceImpl dataService;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    logger.info("Anomaly Writer initialized.");
  }

  @Override
  public void write(List<? extends Anomaly> anomaliesList) throws Exception {
    dataService.writeAnomaliesToTimescale((List<Anomaly>) anomaliesList);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    logger.debug("Anomaly Writer ended.");
    return ExitStatus.COMPLETED;
  }
}
