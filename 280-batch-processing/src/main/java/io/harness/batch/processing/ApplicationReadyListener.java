package io.harness.batch.processing;

import static io.harness.event.app.EventServiceApplication.EVENTS_STORE;

import static com.google.common.base.Verify.verify;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.mongo.IndexManager;
import io.harness.persistence.HPersistence;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
public class ApplicationReadyListener {
  private final TimeScaleDBService timeScaleDBService;
  private final Morphia morphia;
  private final HPersistence hPersistence;
  private final IndexManager indexManager;
  private final Environment environment;

  public ApplicationReadyListener(TimeScaleDBService timeScaleDBService, HPersistence hPersistence, Morphia morphia,
      IndexManager indexManager, Environment environment) {
    this.timeScaleDBService = timeScaleDBService;
    this.hPersistence = hPersistence;
    this.morphia = morphia;
    this.indexManager = indexManager;
    this.environment = environment;
  }

  @EventListener(ApplicationReadyEvent.class)
  void ensureTimescaleConnectivity() {
    if (Boolean.TRUE.equals(environment.getProperty("ensure-timescale", Boolean.class, Boolean.TRUE))) {
      verify(timeScaleDBService.isValid(), "Unable to connect to timescale db");
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  void ensureIndexForEventsStore(ApplicationReadyEvent applicationReadyEvent) {
    AdvancedDatastore datastore = hPersistence.getDatastore(EVENTS_STORE);
    IndexManager.Mode indexManagerMode = applicationReadyEvent.getApplicationContext()
                                             .getBean(BatchMainConfig.class)
                                             .getEventsMongo()
                                             .getIndexManagerMode();
    indexManager.ensureIndexes(indexManagerMode, datastore, morphia, EVENTS_STORE);
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(Ordered.HIGHEST_PRECEDENCE)
  void ensureMongoConnectivity() throws Exception {
    TimeLimiter timeLimiter = new SimpleTimeLimiter();
    try {
      timeLimiter.callWithTimeout(() -> {
        hPersistence.isHealthy();
        return null;
      }, hPersistence.healthExpectedResponseTimeout().toMillis(), TimeUnit.MILLISECONDS, true);
    } catch (UncheckedTimeoutException e) {
      log.error("Timed out waiting for mongo connectivity");
      throw e;
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  void createLivenessMarker() throws IOException {
    File livenessMarker = new File("batch-processing-up");
    boolean created = livenessMarker.createNewFile();
    if (created) {
      log.info("Created liveness marker");
    } else {
      log.error("Failed to create liveness marker");
    }
  }
}
