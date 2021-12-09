package io.harness.telemetry;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.security.SecureRandom;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class NGTelemetryRecordsJob {
  public static final int METRICS_RECORD_PERIOD_HOURS = 1;

  @Inject private Injector injector;
  @Inject @Named("ngTelemetryPublisherExecutor") protected ScheduledExecutorService executorService;
  @Inject NGTelemetryPublisher publisher;

  public void scheduleTasks() {
    long initialDelay = new SecureRandom().nextInt(1);

    try {
      executorService.scheduleAtFixedRate(
          () -> publisher.recordTelemetry(), initialDelay, METRICS_RECORD_PERIOD_HOURS, TimeUnit.MINUTES);
    } catch (Exception e) {
      log.error("Exception while creating the scheduled job", e);
    }
  }
}
