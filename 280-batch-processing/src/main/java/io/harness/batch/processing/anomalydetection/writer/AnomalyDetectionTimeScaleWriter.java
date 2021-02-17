package io.harness.batch.processing.anomalydetection.writer;

import io.harness.ccm.anomaly.entities.Anomaly;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;

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
  @Autowired private AnomalyService anomalyService;

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
    anomalyService.insert(anomalyArrayList);
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    log.info("Anomaly Writer ended.");
    return ExitStatus.COMPLETED;
  }
}
