package io.harness.perpetualtask.internal;

import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofSeconds;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;

import java.time.Instant;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RecentlyDisconnectedDelegateHandler implements Handler<Delegate> {
  @Inject PerpetualTaskRecordDao perpetualTaskRecordDao;

  public static class RecentlyDisconnectedDelegateExecutor {
    static int POOL_SIZE = 3;
    static long ITERATOR_PERIOD_SECOND = 60; // this config can be lowered for testing purpose
    static long DELEGATE_TIMEOUT = TimeUnit.HOURS.toMillis(2);

    public static void registerIterators(Injector injector) {
      final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
          POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-DelegateProcessor").build());
      final RecentlyDisconnectedDelegateHandler handler = new RecentlyDisconnectedDelegateHandler();
      injector.injectMembers(handler);

      PersistenceIterator iterator = MongoPersistenceIterator.<Delegate>builder()
                                         .clazz(Delegate.class)
                                         .fieldName(DelegateKeys.nextRecentlyDisconnectedIteration)
                                         .targetInterval(ofSeconds(ITERATOR_PERIOD_SECOND))
                                         .acceptableNoAlertDelay(ofSeconds(45))
                                         .executorService(executor)
                                         .semaphore(new Semaphore(POOL_SIZE))
                                         .handler(handler)
                                         .filterExpander(q
                                             -> q.and(q.criteria(DelegateKeys.connected).equal(false),
                                                 q.criteria(DelegateKeys.lastHeartBeat)
                                                     .greaterThan(Instant.now().toEpochMilli() - DELEGATE_TIMEOUT)))
                                         .schedulingType(REGULAR)
                                         .redistribute(true)
                                         .build();

      injector.injectMembers(iterator);
      executor.scheduleAtFixedRate(
          () -> iterator.process(ProcessMode.PUMP), 0, ITERATOR_PERIOD_SECOND, TimeUnit.SECONDS);
    }
  }

  @Override
  public void handle(Delegate delegate) {
    perpetualTaskRecordDao.resetDelegateId(delegate.getAccountId(), delegate.getUuid());
  }
}
