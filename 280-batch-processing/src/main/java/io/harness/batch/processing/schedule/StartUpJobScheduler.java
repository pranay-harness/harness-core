package io.harness.batch.processing.schedule;

import static io.harness.batch.processing.ApplicationReadyListener.createLivenessMarker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
public class StartUpJobScheduler {
  /**
   * Created this job because while running functional test
   * ApplicationReadyListener#createLivenessMarkerOnReadyEvent() is not executed.
   */
  @Scheduled(fixedDelay = Long.MAX_VALUE)
  public void createLivenessMarkerJob() {
    log.info("Inside createLivenessMarker");

    try {
      createLivenessMarker();
    } catch (Exception ex) {
      log.error("StartUpJobs: Failed to create liveness marker");
    }
  }
}
