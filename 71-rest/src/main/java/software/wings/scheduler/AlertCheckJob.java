package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.scheduler.PersistentScheduler;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.InvalidSMTPConfigAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtil;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author brett on 10/17/17
 */
public class AlertCheckJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(AlertCheckJob.class);

  public static final String GROUP = "ALERT_CHECK_CRON_GROUP";
  public static final String ACCOUNT_ID = "accountId";

  private static final int POLL_INTERVAL = 300;
  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  private static boolean sendEmailForNoActiveDelegates;

  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateService delegateService;
  @Inject private EmailHelperUtil emailHelperUtil;
  @Inject private MainConfiguration mainConfiguration;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  @Inject private ExecutorService executorService;

  public static void add(PersistentScheduler jobScheduler, Account account) {
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
    executorService.submit(
        () -> executeInternal((String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID)));
  }

  private void executeInternal(String accountId) {
    logger.info("Checking account " + accountId + " for alert conditions.");
    List<Delegate> delegates = getDelegatesForAccount(accountId);

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
      if (!sendEmailForNoActiveDelegates) {
        delegateService.sendAlertNotificationsForNoActiveDelegates(accountId);
        sendEmailForNoActiveDelegates = true;
      }
    } else {
      // reset this flag as all delegates are not down
      sendEmailForNoActiveDelegates = false;
      checkIfAnyDelegatesAreDown(accountId, delegates);
    }
    checkForInvalidValidSMTP(accountId);
  }

  List<Delegate> getDelegatesForAccount(String accountId) {
    return wingsPersistence.createQuery(Delegate.class).filter(Delegate.ACCOUNT_ID_KEY, accountId).asList();
  }

  private void checkForInvalidValidSMTP(String accountId) {
    if (!emailHelperUtil.isSmtpConfigValid(mainConfiguration.getSmtpConfig())
        && !emailHelperUtil.isSmtpConfigValid(emailHelperUtil.getSmtpConfig(accountId))) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.INVALID_SMTP_CONFIGURATION,
          InvalidSMTPConfigAlert.builder().accountId(accountId).build());
    } else {
      alertService.closeAlertsOfType(accountId, GLOBAL_APP_ID, AlertType.INVALID_SMTP_CONFIGURATION);
    }
  }

  /**
   * If any delegate hasn't sent heartbeat for last MAX_HB_TIMEOUT (5 mins currently),
   * raise a dashboard alert
   */
  @SuppressFBWarnings("RpC_REPEATED_CONDITIONAL_TEST")
  private void checkIfAnyDelegatesAreDown(String accountId, List<Delegate> delegates) {
    List<Delegate> delegatesDown =
        delegates.stream()
            .filter(delegate -> System.currentTimeMillis() - delegate.getLastHeartBeat() > MAX_HB_TIMEOUT)
            .collect(toList());

    List<Delegate> delegatesUp =
        delegates.stream()
            .filter(delegate -> System.currentTimeMillis() - delegate.getLastHeartBeat() <= MAX_HB_TIMEOUT)
            .collect(toList());

    if (CollectionUtils.isNotEmpty(delegatesDown)) {
      delegateService.sendAlertNotificationsForDownDelegates(accountId, delegatesDown);
    }

    if (CollectionUtils.isNotEmpty(delegatesUp)) {
      // close if any alert is open
      if (CollectionUtils.isNotEmpty(delegatesUp)) {
        delegatesUp.forEach(delegate
            -> alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown,
                DelegatesDownAlert.builder()
                    .accountId(accountId)
                    .hostName(delegate.getHostName())
                    .ip(delegate.getIp())
                    .build()));
      }
    }
  }
}
