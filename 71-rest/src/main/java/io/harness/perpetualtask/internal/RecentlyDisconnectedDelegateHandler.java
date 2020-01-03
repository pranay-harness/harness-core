package io.harness.perpetualtask.internal;

import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import com.codahale.metrics.InstrumentedExecutorService;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.metrics.HarnessMetricRegistry;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;

import java.time.Instant;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RecentlyDisconnectedDelegateHandler implements Handler<Delegate> {
  private static final int POOL_SIZE = 3;
  private static final long ITERATOR_INTERVAL_MINUTE = 5;
  private static final long DELEGATE_TIMEOUT = TimeUnit.HOURS.toMillis(2);

  @Inject PerpetualTaskRecordDao perpetualTaskRecordDao;
  @Inject HarnessMetricRegistry harnessMetricRegistry;

  public void registerIterators() {
    String name = "Iterator-DelegateProcessor";
    final ScheduledThreadPoolExecutor executor =
        new ScheduledThreadPoolExecutor(POOL_SIZE, new ThreadFactoryBuilder().setNameFormat(name).build());
    InstrumentedExecutorService instrumentedExecutorService =
        new InstrumentedExecutorService(executor, harnessMetricRegistry.getThreadPoolMetricRegistry(), name);

    PersistenceIterator iterator = MongoPersistenceIterator.<Delegate>builder()
                                       .clazz(Delegate.class)
                                       .fieldName(DelegateKeys.nextRecentlyDisconnectedIteration)
                                       .targetInterval(ofMinutes(ITERATOR_INTERVAL_MINUTE))
                                       .acceptableNoAlertDelay(ofSeconds(45))
                                       .executorService(instrumentedExecutorService)
                                       .semaphore(new Semaphore(POOL_SIZE))
                                       .handler(this)
                                       .filterExpander(q
                                           -> q.and(q.criteria(DelegateKeys.connected).equal(false),
                                               q.criteria(DelegateKeys.lastHeartBeat)
                                                   .greaterThan(Instant.now().toEpochMilli() - DELEGATE_TIMEOUT)))
                                       .schedulingType(REGULAR)
                                       .redistribute(true)
                                       .build();

    executor.scheduleAtFixedRate(
        () -> iterator.process(ProcessMode.PUMP), 0, ITERATOR_INTERVAL_MINUTE, TimeUnit.MINUTES);
  }

  @Override
  public void handle(Delegate delegate) {
    perpetualTaskRecordDao.resetDelegateId(delegate.getAccountId(), delegate.getUuid());
  }
}
