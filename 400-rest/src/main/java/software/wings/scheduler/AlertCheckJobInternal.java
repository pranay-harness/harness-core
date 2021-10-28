package software.wings.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.obfuscate.Obfuscator.obfuscate;

import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ManagerConfiguration.MATCH_ALL_VERSION;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.alert.DelegatesScalingGroupDownAlert;
import io.harness.scheduler.PersistentScheduler;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.ManagerConfiguration;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.InvalidSMTPConfigAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoInstalledDelegatesAlert;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateConnectionDao;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.EmailHelperUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

// todo(abhinav): we will have to remove alertService at some point and either have alert client or alertsvc depending
// on manaeger or dms mode.
@OwnedBy(HarnessTeam.DEL)
@Slf4j
public class AlertCheckJobInternal {
  @Inject private AlertService alertService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private EmailHelperUtils emailHelperUtils;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private DelegateService delegateService;
  @Inject @Named("BackgroundJobScheduler") private PersistentScheduler jobScheduler;

  public void executeInternal(String accountId) {
    log.info("Checking account " + accountId + " for alert conditions.");
    List<Delegate> delegates = delegateService.getNonDeletedDelegatesForAccount(accountId);

    if (isEmpty(delegates)) {
      Account account = wingsPersistence.get(Account.class, accountId);
      if (account == null) {
        jobScheduler.deleteJob(accountId, AlertCheckJob.GROUP);
        return;
      }
    }
    if (isEmpty(delegates)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoInstalledDelegates,
          NoInstalledDelegatesAlert.builder().accountId(accountId).build());
    } else if (delegates.stream().allMatch(delegate
                   -> System.currentTimeMillis() - delegate.getLastHeartBeat() > AlertCheckJob.MAX_HB_TIMEOUT)) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.NoActiveDelegates,
          NoActiveDelegatesAlert.builder().accountId(accountId).build());
    } else {
      checkIfAnyDelegatesAreDown(accountId, delegates);
    }
    checkForInvalidValidSMTP(accountId);
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
    String primaryVersion = wingsPersistence.createQuery(ManagerConfiguration.class).get().getPrimaryVersion();
    Set<String> primaryConnections =
        delegateConnectionDao.obtainConnectedDelegates(accountId, primaryVersion, MATCH_ALL_VERSION);

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
