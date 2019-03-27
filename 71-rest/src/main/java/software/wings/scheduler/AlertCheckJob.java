package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ManagerConfiguration.MATCH_ALL_VERSION;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.scheduler.PersistentScheduler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection;
import software.wings.beans.ManagerConfiguration;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.InvalidSMTPConfigAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtil;

import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author brett on 10/17/17
 */
public class AlertCheckJob implements Job {
  private static final Logger logger = LoggerFactory.getLogger(AlertCheckJob.class);

  public static final String GROUP = "ALERT_CHECK_CRON_GROUP";

  private static final int POLL_INTERVAL = 300;
  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
  private static boolean sendEmailForNoActiveDelegates;

  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DelegateService delegateService;
  @Inject private EmailHelperUtil emailHelperUtil;
  @Inject private MainConfiguration mainConfiguration;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private Clock clock;

  @Inject private ExecutorService executorService;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + new Random().nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
    addInternal(jobScheduler, accountId, new Date(startTime));
  }

  public static void add(PersistentScheduler jobScheduler, String accountId) {
    addInternal(jobScheduler, accountId, null);
  }

  private static void addInternal(PersistentScheduler jobScheduler, String accountId, Date triggerStartTime) {
    JobDetail job = JobBuilder.newJob(AlertCheckJob.class)
                        .withIdentity(accountId, GROUP)
                        .usingJobData(ACCOUNT_ID_KEY, accountId)
                        .build();

    TriggerBuilder triggerBuilder =
        TriggerBuilder.newTrigger()
            .withIdentity(accountId, GROUP)
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(POLL_INTERVAL).repeatForever());
    if (triggerStartTime != null) {
      triggerBuilder.startAt(triggerStartTime);
    }

    jobScheduler.ensureJob__UnderConstruction(job, triggerBuilder.build());
  }

  public static void delete(PersistentScheduler jobScheduler, String accountId) {
    jobScheduler.deleteJob(accountId, GROUP);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    executorService.submit(
        () -> executeInternal((String) jobExecutionContext.getJobDetail().getJobDataMap().get(ACCOUNT_ID_KEY)));
  }

  @VisibleForTesting
  void executeInternal(String accountId) {
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

  @VisibleForTesting
  void checkForInvalidValidSMTP(String accountId) {
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
  private void checkIfAnyDelegatesAreDown(String accountId, List<Delegate> delegates) {
    Query<DelegateConnection> query =
        wingsPersistence.createQuery(DelegateConnection.class).filter(DelegateConnection.ACCOUNT_ID_KEY, accountId);
    String primaryVersion = wingsPersistence.createQuery(ManagerConfiguration.class).get().getPrimaryVersion();
    if (isNotEmpty(primaryVersion) && !StringUtils.equals(primaryVersion, MATCH_ALL_VERSION)) {
      query.filter("version", primaryVersion);
    }
    Map<String, DelegateConnection> primaryConnections =
        query.project("delegateId", true)
            .project("lastHeartbeat", true)
            .asList()
            .stream()
            .collect(toMap(DelegateConnection::getDelegateId, connection -> connection));

    List<Delegate> delegatesDown =
        delegates.stream()
            .filter(delegate -> primaryConnections.containsKey(delegate.getUuid()))
            .filter(delegate
                -> clock.millis() - primaryConnections.get(delegate.getUuid()).getLastHeartbeat() > MAX_HB_TIMEOUT)
            .collect(toList());

    List<Delegate> delegatesUp =
        delegates.stream()
            .filter(delegate -> primaryConnections.containsKey(delegate.getUuid()))
            .filter(delegate
                -> clock.millis() - primaryConnections.get(delegate.getUuid()).getLastHeartbeat() <= MAX_HB_TIMEOUT)
            .collect(toList());

    if (CollectionUtils.isNotEmpty(delegatesDown)) {
      delegateService.sendAlertNotificationsForDownDelegates(accountId, delegatesDown);
    }

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
