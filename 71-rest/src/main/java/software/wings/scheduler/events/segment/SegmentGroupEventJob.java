package software.wings.scheduler.events.segment;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.mongo.MongoPersistenceIterator.SchedulingType.REGULAR;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.event.handler.impl.account.AccountChangeHandler;
import io.harness.iterator.PersistenceIterator;
import io.harness.iterator.PersistenceIterator.ProcessMode;
import io.harness.mongo.MongoPersistenceIterator;
import io.harness.mongo.MongoPersistenceIterator.Handler;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext.SegmentGroupEventJobContextKeys;
import software.wings.service.intfc.AccountService;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Publishes `group` events to segment for all accounts at regular intervals.
 */
@Slf4j
public class SegmentGroupEventJob implements Handler<SegmentGroupEventJobContext> {
  @Inject private AccountChangeHandler accountChangeHandler;
  @Inject private AccountService accountService;

  public static class SegmentGroupEventJobExecutor {
    static int POOL_SIZE = 2;
    static Duration INTERVAL = Duration.ofHours(24);
    static Duration ACCEPTABLE_DELAY = Duration.ofMinutes(35);
    static Duration CHECK_INTERVAL = Duration.ofMinutes(30);

    public static void registerIterators(Injector injector) {
      try {
        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
            POOL_SIZE, new ThreadFactoryBuilder().setNameFormat("Iterator-SegmentGroupEventJob-Pool").build());

        final SegmentGroupEventJob handler = new SegmentGroupEventJob();
        injector.injectMembers(handler);

        Semaphore semaphore = new Semaphore(POOL_SIZE);
        PersistenceIterator iterator = MongoPersistenceIterator.<SegmentGroupEventJobContext>builder()
                                           .clazz(SegmentGroupEventJobContext.class)
                                           .fieldName(SegmentGroupEventJobContextKeys.nextIteration)
                                           .targetInterval(INTERVAL)
                                           .acceptableNoAlertDelay(ACCEPTABLE_DELAY)
                                           .executorService(executor)
                                           .semaphore(semaphore)
                                           .handler(handler)
                                           .schedulingType(REGULAR)
                                           .redistribute(true)
                                           .build();

        injector.injectMembers(iterator);

        final Random rand = new Random();

        // this'll check every 30 minutes if there are any new jobs to process.
        // this value must be lower than `targetInterval`
        executor.scheduleAtFixedRate(()
                                         -> iterator.process(ProcessMode.PUMP),
            rand.nextInt((int) CHECK_INTERVAL.getSeconds()), CHECK_INTERVAL.getSeconds(), TimeUnit.SECONDS);
      } catch (Exception e) {
        logger.error("Error registering iterators in SegmentGroupEventJob", e);
      }
    }
  }

  @Override
  public void handle(SegmentGroupEventJobContext entity) {
    PageRequest<Account> request =
        aPageRequest().addFilter("_id", Operator.IN, entity.getAccountIds().toArray()).build();

    List<Account> accounts = accountService.list(request);

    logger.info("Segment publish job with accounts. count={}", accounts.size());
    for (Account account : accounts) {
      if (Account.GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
        continue;
      }

      logger.info("publishing segment group event. accountId={}", account.getUuid());
      accountChangeHandler.publishAccountEventToSegment(account);
    }
  }
}
