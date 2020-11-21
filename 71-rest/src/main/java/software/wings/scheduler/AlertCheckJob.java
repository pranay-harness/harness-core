package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.obfuscate.Obfuscator.obfuscate;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.Constants.ACCOUNT_ID_KEY;

import io.harness.alert.AlertData;
import io.harness.delegate.beans.alert.DelegatesScalingGroupDownAlert;
import io.harness.scheduler.PersistentScheduler;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateConnection.DelegateConnectionKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.InvalidSMTPConfigAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoInstalledDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateConnectionDao;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.utils.EmailHelperUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.TriggerBuilder;

/**
 * @author brett on 10/17/17
 */
@Slf4j
public class AlertCheckJob implements Job {
  private static final SecureRandom random = new SecureRandom();
  public static final String GROUP = "ALERT_CHECK_CRON_GROUP";

  private static final int POLL_INTERVAL = 300;
  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(5);

  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EmailHelperUtils emailHelperUtils;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private FeatureFlagService featureFlagService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;
  @Inject private Clock clock;

  @Inject private ExecutorService executorService;

  public static void addWithDelay(PersistentScheduler jobScheduler, String accountId) {
    // Add some randomness in the trigger start time to avoid overloading quartz by firing jobs at the same time.
    long startTime = System.currentTimeMillis() + random.nextInt((int) TimeUnit.SECONDS.toMillis(POLL_INTERVAL));
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
    log.info("Checking account " + accountId + " for alert conditions.");
    List<Delegate> delegates = getDelegatesForAccount(accountId);

    if (isEmpty(delegates)) {
      Account account = wingsPersistence.get(Account.class, accountId);
      if (account == null) {
        jobScheduler.deleteJob(accountId, GROUP);
        return;
      }
    }
    if (isEmpty(delegates)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoInstalledDelegates,
          NoInstalledDelegatesAlert.builder().accountId(accountId).build());
    } else if (delegates.stream().allMatch(
                   delegate -> System.currentTimeMillis() - delegate.getLastHeartBeat() > MAX_HB_TIMEOUT)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(accountId).build());
    } else {
      checkIfAnyDelegatesAreDown(accountId, delegates);
    }
    checkForInvalidValidSMTP(accountId);
  }

  private List<Delegate> getDelegatesForAccount(String accountId) {
    return wingsPersistence.createQuery(Delegate.class).filter(DelegateConnectionKeys.accountId, accountId).asList();
  }

  @VisibleForTesting
  void checkForInvalidValidSMTP(String accountId) {
    if (!emailHelperUtils.isSmtpConfigValid(mainConfiguration.getSmtpConfig())
        && !emailHelperUtils.isSmtpConfigValid(emailHelperUtils.getSmtpConfig(accountId))) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.INVALID_SMTP_CONFIGURATION,
          InvalidSMTPConfigAlert.builder().accountId(accountId).build());
    } else {
      alertService.closeAlertsOfType(accountId, GLOBAL_APP_ID, AlertType.INVALID_SMTP_CONFIGURATION);
    }
  }

  private void checkIfAnyDelegatesAreDown(String accountId, List<Delegate> delegates) {
    Set<String> primaryConnections = delegateConnectionDao.obtainConnectedDelegates(accountId);

    for (Delegate delegate : delegates) {
      AlertData alertData = DelegatesDownAlert.builder()
                                .accountId(accountId)
                                .hostName(delegate.getHostName())
                                .obfuscatedIpAddress(obfuscate(delegate.getIp()))
                                .build();

      if (primaryConnections.contains(delegate.getUuid())) {
        alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown, alertData);
      } else {
        if (isEmpty(delegate.getDelegateGroupName())) {
          alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown, alertData);
        }
      }
    }

    processDelegateWhichBelongsToGroup(accountId, delegates, primaryConnections);
  }

  @VisibleForTesting
  protected void processDelegateWhichBelongsToGroup(
      String accountId, List<Delegate> delegates, Set<String> primaryConnections) {
    Set<String> connectedScalingGroups = new HashSet<>();
    for (Delegate delegate : delegates) {
      if (primaryConnections.contains(delegate.getUuid()) && isNotEmpty(delegate.getDelegateGroupName())) {
        String delegateGroupName = delegate.getDelegateGroupName();
        closeDelegateScalingGroupDownAlert(accountId, delegateGroupName);
        connectedScalingGroups.add(delegateGroupName);
      }
    }

    Set<String> allScalingGroups = delegates.stream()
                                       .filter(x -> isNotEmpty(x.getDelegateGroupName()))
                                       .map(Delegate::getDelegateGroupName)
                                       .collect(Collectors.toSet());

    allScalingGroups.removeAll(connectedScalingGroups);

    for (String disconnectedScalingGroup : allScalingGroups) {
      AlertData alertData =
          DelegatesScalingGroupDownAlert.builder().accountId(accountId).groupName(disconnectedScalingGroup).build();

      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesScalingGroupDownAlert, alertData);
    }
  }

  private void closeDelegateScalingGroupDownAlert(String accountId, String groupName) {
    AlertData alertData = DelegatesScalingGroupDownAlert.builder().accountId(accountId).groupName(groupName).build();
    alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesScalingGroupDownAlert, alertData);
  }
}
