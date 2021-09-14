/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.background;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.execution.export.ExportExecutionsRequestLogContext;
import io.harness.execution.export.request.ExportExecutionsRequest;
import io.harness.execution.export.request.ExportExecutionsRequest.ExportExecutionsRequestKeys;
import io.harness.execution.export.request.ExportExecutionsRequest.Status;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ExportExecutionsRequestHandler implements Handler<ExportExecutionsRequest> {
  private static final int ASSIGNMENT_INTERVAL_MINUTES = 1;
  private static final int REASSIGNMENT_INTERVAL_MINUTES = 30;
  private static final int ACCEPTABLE_DELAY_MINUTES = 10;

  @Inject private AccountService accountService;
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private ExportExecutionsService exportExecutionsService;
  @Inject private ExportExecutionsNotificationHelper exportExecutionsNotificationHelper;
  @Inject private MorphiaPersistenceProvider<ExportExecutionsRequest> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("ExportExecutionsRequestHandler")
            .poolSize(3)
            .interval(ofMinutes(ASSIGNMENT_INTERVAL_MINUTES))
            .build(),
        ExportExecutionsRequestHandler.class,
        MongoPersistenceIterator.<ExportExecutionsRequest, MorphiaFilterExpander<ExportExecutionsRequest>>builder()
            .clazz(ExportExecutionsRequest.class)
            .fieldName(ExportExecutionsRequestKeys.nextIteration)
            .targetInterval(ofMinutes(REASSIGNMENT_INTERVAL_MINUTES))
            .acceptableNoAlertDelay(ofMinutes(ACCEPTABLE_DELAY_MINUTES))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .filterExpander(query -> query.field(ExportExecutionsRequestKeys.status).equal(Status.QUEUED))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(ExportExecutionsRequest request) {
    if (request == null) {
      log.warn("ExportExecutionsRequest is null");
      return;
    }

    try (AutoLogContext ignore1 = new AccountLogContext(request.getAccountId(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ExportExecutionsRequestLogContext(request.getUuid(), OVERRIDE_ERROR)) {
      try {
        exportExecutionsService.export(request);
      } catch (Exception ex) {
        log.error("Unable to process export executions request", ex);

        try {
          exportExecutionsService.failRequest(request, ExceptionUtils.getMessage(ex));
        } catch (Exception ex1) {
          log.error("Unable to fail export executions request", ex1);
          return;
        }
      }

      exportExecutionsNotificationHelper.dispatch(request);
    }
  }
}
