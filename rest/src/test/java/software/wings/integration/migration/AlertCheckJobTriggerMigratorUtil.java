package software.wings.integration.migration;

import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.scheduler.AlertCheckJob;
import software.wings.scheduler.QuartzScheduler;

/**
 * @author brett on 10/17/17
 */
@Integration
@Ignore
public class AlertCheckJobTriggerMigratorUtil extends WingsBaseTest {
  private static final String ALERT_CHECK_CRON_GROUP = "ALERT_CHECK_CRON_GROUP";
  private static final int ALERT_CHECK_POLL_INTERVAL = 300;

  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  /**
   * Run this test by specifying VM argument -DsetupScheduler="true"
   */
  @Test
  public void scheduleCronForAlertCheck() {
    PageRequest<Account> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving accounts");
    PageResponse<Account> pageResponse = wingsPersistence.query(Account.class, pageRequest);

    if (pageResponse.isEmpty() || CollectionUtils.isEmpty(pageResponse.getResponse())) {
      System.out.println("No accounts found");
      return;
    }
    pageResponse.getResponse().forEach(account -> {
      System.out.println("Creating alert check scheduler for account " + account.getUuid());
      // deleting the old
      jobScheduler.deleteJob(account.getUuid(), ALERT_CHECK_CRON_GROUP);
      addCronForAlertCheck(account);
    });
  }

  private void addCronForAlertCheck(Account account) {
    JobDetail job = JobBuilder.newJob(AlertCheckJob.class)
                        .withIdentity(account.getUuid(), ALERT_CHECK_CRON_GROUP)
                        .usingJobData("accountId", account.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(account.getUuid(), ALERT_CHECK_CRON_GROUP)
            .withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(ALERT_CHECK_POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }
}
