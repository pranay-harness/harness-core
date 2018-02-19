package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author brett on 10/17/17
 */
public class AlertCheckJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(AlertCheckJob.class);

  public static final String GROUP = "ALERT_CHECK_CRON_GROUP";
  public static final int POLL_INTERVAL = 300;

  public static final String ACCOUNT_ID = "accountId";

  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

  @Inject private ExecutorService executorService;

  public static void add(QuartzScheduler jobScheduler, Account account) {
    jobScheduler.deleteJob(account.getUuid(), GROUP);
    JobDetail job = JobBuilder.newJob(AlertCheckJob.class)
                        .withIdentity(account.getUuid(), GROUP)
                        .usingJobData(ACCOUNT_ID, account.getUuid())
                        .build();

    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity(account.getUuid(), GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever())
            .build();

    jobScheduler.scheduleJob(job, trigger);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    logger.info("Running AlertCheck Job asynchronously and returning");
    executorService.submit(
        () -> executeInternal((String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID)));
  }

  private void executeInternal(String accountId) {
    logger.info("Checking account " + accountId + " for alert conditions.");
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).field(Delegate.ACCOUNT_ID_KEY).equal(accountId).asList();

    if (isEmpty(delegates)) {
      Account account = wingsPersistence.get(Account.class, accountId);
      if (account == null) {
        jobScheduler.deleteJob(accountId, GROUP);
        return;
      }
    }
    if (isEmpty(delegates)
        || delegates.stream().allMatch(
               delegate -> System.currentTimeMillis() - delegate.getLastHeartBeat() > MAX_HB_TIMEOUT)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(accountId).build());
    }
  }
}
