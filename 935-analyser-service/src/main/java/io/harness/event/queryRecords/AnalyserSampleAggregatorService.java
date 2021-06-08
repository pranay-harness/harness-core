package io.harness.event.queryRecords;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.repositories.QueryRecordsRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
public class AnalyserSampleAggregatorService implements Runnable {
  @Inject private QueryRecordsRepository queryRecordsRepository;

  @Override
  public void run() {
    try {
      execute();
    } catch (Exception e) {
      log.error("Exception happened in SampleAggregator execute", e);
    }
  }

  public void execute() {
    try {
    } catch (WingsException exception) {
      ExceptionLogger.logProcessedMessages(exception, MANAGER, log);
    } catch (Exception exception) {
      log.error("Error seen in the SampleAggregator call", exception);
    }
  }
}
