package io.harness.perpetualtask.internal;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.internal.PerpetualTaskRecord.PerpetualTaskRecordKeys;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.service.intfc.DelegateService;

@Slf4j
public class DisconnectedDelegateHandler implements Handler<PerpetualTaskRecord> {
  private static final long ITERATOR_INTERVAL_MINUTE = 5;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private PerpetualTaskService perpetualTaskService;
  @Inject private DelegateService delegateService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("PerpetualTaskRecordProcessor")
            .poolSize(3)
            .interval(ofMinutes(ITERATOR_INTERVAL_MINUTE))
            .build(),
        Delegate.class,
        MongoPersistenceIterator.<PerpetualTaskRecord>builder()
            .clazz(PerpetualTaskRecord.class)
            .fieldName(PerpetualTaskRecordKeys.resetterIteration)
            .targetInterval(ofMinutes(ITERATOR_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(45))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(PerpetualTaskRecord pTask) {
    if (!delegateService.checkDelegateConnected(pTask.getAccountId(), pTask.getDelegateId())) {
      logger.info("Resetting perpetual tasks assigned to disconnected delegate with id={}", pTask.getDelegateId());
      perpetualTaskService.resetTask(pTask.getAccountId(), pTask.getUuid());
    }
  }
}
