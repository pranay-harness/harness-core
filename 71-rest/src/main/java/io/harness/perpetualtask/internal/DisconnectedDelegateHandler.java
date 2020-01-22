package io.harness.perpetualtask.internal;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.inject.Inject;

import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.service.intfc.DelegateService;

@Slf4j
public class DisconnectedDelegateHandler implements Handler<Delegate> {
  private static final long ITERATOR_INTERVAL_MINUTE = 5;

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Inject private DelegateService delegateService;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder()
            .name("DelegateProcessor")
            .poolSize(3)
            .interval(ofMinutes(ITERATOR_INTERVAL_MINUTE))
            .build(),
        Delegate.class,
        MongoPersistenceIterator.<Delegate>builder()
            .clazz(Delegate.class)
            .fieldName(DelegateKeys.nextRecentlyDisconnectedIteration)
            .targetInterval(ofMinutes(ITERATOR_INTERVAL_MINUTE))
            .acceptableNoAlertDelay(ofSeconds(45))
            .handler(this)
            .schedulingType(REGULAR)
            .redistribute(true));
  }

  @Override
  public void handle(Delegate delegate) {
    if (!delegateService.isDelegateConnected(delegate)) {
      logger.info("Resetting perpetual tasks assigned to disconnected delegate with id={}", delegate.getUuid());
      perpetualTaskRecordDao.resetDelegateId(delegate.getAccountId(), delegate.getUuid());
    }
  }
}
