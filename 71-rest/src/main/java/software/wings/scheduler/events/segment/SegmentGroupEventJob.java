package software.wings.scheduler.events.segment;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;
import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;
import io.harness.event.handler.impl.account.AccountChangeHandler;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.iterator.PersistenceIteratorFactory.PumpExecutorOptions;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.MongoPersistenceIterator.Handler;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext.SegmentGroupEventJobContextKeys;
import software.wings.service.intfc.AccountService;

import java.util.List;

/**
 * Publishes `group` events to segment for all accounts at regular intervals.
 */
@Slf4j
public class SegmentGroupEventJob implements Handler<SegmentGroupEventJobContext> {
  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private AccountChangeHandler accountChangeHandler;
  @Inject private AccountService accountService;
  @Inject private MorphiaPersistenceProvider<SegmentGroupEventJobContext> persistenceProvider;

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PumpExecutorOptions.builder().name("SegmentGroupEventJob").poolSize(2).interval(ofMinutes(30)).build(),
        SegmentGroupEventJob.class,
        MongoPersistenceIterator
            .<SegmentGroupEventJobContext, MorphiaFilterExpander<SegmentGroupEventJobContext>>builder()
            .clazz(SegmentGroupEventJobContext.class)
            .fieldName(SegmentGroupEventJobContextKeys.nextIteration)
            .targetInterval(ofHours(24))
            .acceptableNoAlertDelay(ofMinutes(35))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(SegmentGroupEventJobContext entity) {
    PageRequest<Account> request =
        aPageRequest().addFilter("_id", Operator.IN, entity.getAccountIds().toArray()).build();

    List<Account> accounts = accountService.list(request);

    log.info("Segment publish job with accounts. count={}", accounts.size());
    for (Account account : accounts) {
      if (Account.GLOBAL_ACCOUNT_ID.equals(account.getUuid())) {
        continue;
      }

      if (AccountStatus.ACTIVE.equals(accountService.getAccountStatus(account.getUuid()))) {
        log.info("publishing segment group event. accountId={}", account.getUuid());
        accountChangeHandler.publishAccountEventToSegment(account);
      }
    }
  }
}
