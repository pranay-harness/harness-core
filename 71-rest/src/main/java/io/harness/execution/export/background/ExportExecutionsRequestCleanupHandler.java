package io.harness.execution.export.background;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.ExportExecutionsRequestLogContext;
import io.harness.execution.export.request.ExportExecutionsRequest;
import io.harness.execution.export.request.ExportExecutionsRequest.ExportExecutionsRequestKeys;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ExportExecutionsRequestCleanupHandler implements Handler<ExportExecutionsRequest> {
  private static final int ASSIGNMENT_INTERVAL_HOURS = 1;
  private static final int REASSIGNMENT_INTERVAL_MINUTES = 45;
  private static final int ACCEPTABLE_DELAY_MINUTES = 45;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private ExportExecutionsService exportExecutionsService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("ExportExecutionsRequestCleanupHandler")
            .poolSize(2)
            .interval(ofHours(ASSIGNMENT_INTERVAL_HOURS))
            .build(),
        ExportExecutionsRequestCleanupHandler.class,
        MongoPersistenceIterator.<ExportExecutionsRequest>builder()
            .clazz(ExportExecutionsRequest.class)
            .fieldName(ExportExecutionsRequestKeys.nextCleanupIteration)
            .targetInterval(ofMinutes(REASSIGNMENT_INTERVAL_MINUTES))
            .acceptableNoAlertDelay(ofMinutes(ACCEPTABLE_DELAY_MINUTES))
            .handler(this)
            .filterExpander(query
                -> query.field(ExportExecutionsRequestKeys.status)
                       .equal(Status.READY)
                       .field(ExportExecutionsRequestKeys.expiresAt)
                       .greaterThan(0)
                       .field(ExportExecutionsRequestKeys.expiresAt)
                       .lessThan(System.currentTimeMillis()))
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(ExportExecutionsRequest request) {
    if (request == null) {
      logger.warn("ExportExecutionsRequest is null");
      return;
    }

    try (AutoLogContext ignore1 = new AccountLogContext(request.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ExportExecutionsRequestLogContext(request.getUuid(), OVERRIDE_ERROR)) {
      try {
        exportExecutionsService.expireRequest(request);
      } catch (Exception ex) {
        // NOTE: This operation will be tried again. Just log an error and don't rethrow exception.
        logger.error("Unable to cleanup export executions request", ex);
      }
    }
  }
}
