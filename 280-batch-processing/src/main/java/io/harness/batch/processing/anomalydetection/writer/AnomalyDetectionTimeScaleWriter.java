package io.harness.batch.processing.anomalydetection.writer;

import io.harness.batch.processing.anomalydetection.service.impl.AnomalyDetectionTimescaleDataServiceImpl;
import io.harness.batch.processing.anomalydetection.types.Anomaly;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AnomalyDetectionTimeScaleWriter implements ItemWriter<Anomaly>, StepExecutionListener {
  @Autowired private AnomalyDetectionTimescaleDataServiceImpl dataService;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    log.info("Anomaly Writer initialized.");
  }

  @Override
  public void write(List<? extends Anomaly> anomaliesList) throws Exception {
    List<Anomaly> anomalyArrayList = new ArrayList<>(anomaliesList);
    ListIterator<Anomaly> iter = anomalyArrayList.listIterator();
    Anomaly currentAnomaly;
    while (iter.hasNext()) {
      currentAnomaly = iter.next();
      if (!currentAnomaly.isAnomaly()) {
        iter.remove();
      }
    }
    dataService.writeAnomaliesToTimescale(anomalyArrayList);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.debug("Anomaly Writer ended.");
    return ExitStatus.COMPLETED;
  }
}
