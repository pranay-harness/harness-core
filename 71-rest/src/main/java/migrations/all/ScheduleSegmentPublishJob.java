package migrations.all;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.event.handler.impl.segment.SegmentGroupEventJobService;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.Account;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.events.segment.SegmentGroupEventJobContext;
import software.wings.service.intfc.AccountService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ScheduleSegmentPublishJob implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AccountService accountService;

  @Override
  public void migrate() {
    try {
      // delete entries if any
      wingsPersistence.delete(wingsPersistence.createQuery(SegmentGroupEventJobContext.class));

      List<Account> accounts = accountService.listAllAccounts();
      List<List<Account>> accountLists = Lists.partition(accounts, SegmentGroupEventJobService.ACCOUNT_BATCH_SIZE);

      Instant nextIteration = Instant.now();

      // schedules a job for each subList. So, one job will have a list of accountIds
      for (List<Account> accountList : accountLists) {
        List<String> accountIds = accountList.stream().map(Account::getUuid).collect(Collectors.toList());

        nextIteration = nextIteration.plus(30, ChronoUnit.MINUTES);
        SegmentGroupEventJobContext jobContext =
            new SegmentGroupEventJobContext(nextIteration.toEpochMilli(), accountIds);
        wingsPersistence.save(jobContext);
      }

    } catch (Exception e) {
      logger.error("Exception scheduling segment job", e);
    }
  }
}
