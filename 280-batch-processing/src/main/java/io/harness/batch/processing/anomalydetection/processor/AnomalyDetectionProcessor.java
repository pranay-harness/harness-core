package io.harness.batch.processing.anomalydetection.processor;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.harness.batch.processing.anomalydetection.AnomalyDetectionTimeSeries;
import io.harness.batch.processing.anomalydetection.helpers.AnomalyDetectionHelper;
import io.harness.batch.processing.anomalydetection.models.StatsModel;
import io.harness.batch.processing.anomalydetection.pythonserviceendpoint.AnomalyDetectionPythonService;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.service.impl.AnomalyDetectionLogContext;
import io.harness.ccm.anomaly.entities.Anomaly;
import io.harness.logging.AutoLogContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AnomalyDetectionProcessor
    implements ItemProcessor<AnomalyDetectionTimeSeries, Anomaly>, StepExecutionListener {
  @Autowired StatsModel statsModel;
  @Autowired AnomalyDetectionPythonService pythonService;
  @Autowired BatchMainConfig mainConfig;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("TimeSeries Processor initialized.");
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("TimeSeries Processor ended.");
    return ExitStatus.COMPLETED;
  }

  @Override
  public Anomaly process(AnomalyDetectionTimeSeries timeSeries) throws Exception {
    Anomaly returnAnomaly;
    Anomaly returnAnomaly2;
    try (AutoLogContext ignore = new AnomalyDetectionLogContext(timeSeries.getId(), OVERRIDE_ERROR)) {
      AnomalyDetectionHelper.logProcessingTimeSeries("Stats Model");
      returnAnomaly = statsModel.detectAnomaly(timeSeries);
      AnomalyDetectionHelper.logProcessingTimeSeries("Prophet Model");
      returnAnomaly2 = pythonService.process(timeSeries);
      log.info("finally after processing, isAnomaly : [{}]", returnAnomaly.isAnomaly());
    }

    return returnAnomaly;
  }
}
