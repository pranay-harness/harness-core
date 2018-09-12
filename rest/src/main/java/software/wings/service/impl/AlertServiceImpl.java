package software.wings.service.impl;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.alerts.AlertStatus.Closed;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.alert.Alert.AlertBuilder.anAlert;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.beans.alert.AlertType.NoActiveDelegates;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class AlertServiceImpl implements AlertService {
  private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private Injector injector;
  @Inject private TimeLimiter timeLimiter;

  @Override
  public PageResponse<Alert> list(PageRequest<Alert> pageRequest) {
    return wingsPersistence.query(Alert.class, pageRequest);
  }

  @Override
  public Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    return executorService.submit(() -> openInternal(accountId, appId, alertType, alertData));
  }

  /**
   * This method opens multiple alerts of the same type.
   * E.g. there can be multiple delegates down at a time (not all but multiple),
   * in this scenario, we want to create 1 alert per delegate of the type "DelegateDown".
   * @param accountId
   * @param appId
   * @param alertType
   * @param alertData
   * @return
   */
  @Override
  public Future openAlerts(String accountId, String appId, AlertType alertType, List<AlertData> alertData) {
    return executorService.submit(() -> openAlertsInternal(accountId, appId, alertType, alertData));
  }

  @Override
  public void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    executorService.submit(() -> findExistingAlert(accountId, appId, alertType, alertData).ifPresent(this ::close));
  }

  @Override
  public void closeAlertsOfType(String accountId, String appId, AlertType alertType) {
    executorService.submit(() -> findExistingAlertsOfType(accountId, appId, alertType).forEach(alert -> close(alert)));
  }

  @Override
  public void activeDelegateUpdated(String accountId, String delegateId) {
    executorService.submit(() -> activeDelegateUpdatedInternal(accountId, delegateId));
  }

  @Override
  public void deploymentCompleted(String appId, String executionId) {
    executorService.submit(() -> deploymentCompletedInternal(appId, executionId));
  }

  private void openAlertsInternal(String accountId, String appId, AlertType alertType, List<AlertData> alertData) {
    alertData.forEach(data -> openInternal(accountId, appId, alertType, data));
  }
  private void openInternal(String accountId, String appId, AlertType alertType, AlertData alertData) {
    String lockName = alertType.name() + "-" + (appId == null || appId.equals(GLOBAL_APP_ID) ? accountId : appId);
    try (AcquiredLock lock = persistentLocker.acquireLock(AlertType.class, lockName, Duration.ofMinutes(1))) {
      if (findExistingAlert(accountId, appId, alertType, alertData).isPresent()) {
        return;
      }
      injector.injectMembers(alertData);
      Alert persistedAlert = wingsPersistence.saveAndGet(Alert.class,
          anAlert()
              .withAppId(appId)
              .withAccountId(accountId)
              .withType(alertType)
              .withStatus(Open)
              .withAlertData(alertData)
              .withTitle(alertData.buildTitle())
              .withCategory(alertType.getCategory())
              .withSeverity(alertType.getSeverity())
              .build());
      logger.info("Alert opened: {}", persistedAlert);
    }
  }

  private void activeDelegateUpdatedInternal(String accountId, String delegateId) {
    findExistingAlert(
        accountId, GLOBAL_APP_ID, NoActiveDelegates, NoActiveDelegatesAlert.builder().accountId(accountId).build())
        .ifPresent(this ::close);
    wingsPersistence.createQuery(Alert.class)
        .filter(Alert.ACCOUNT_ID_KEY, accountId)
        .filter("type", NoEligibleDelegates)
        .filter("status", Open)
        .asList()
        .stream()
        .filter(alert -> {
          NoEligibleDelegatesAlert data = (NoEligibleDelegatesAlert) alert.getAlertData();
          return assignDelegateService.canAssign(delegateId, accountId, data.getAppId(), data.getEnvId(),
              data.getInfraMappingId(), data.getTaskGroup(), data.getTags());
        })
        .forEach(this ::close);
  }

  private void deploymentCompletedInternal(String appId, String executionId) {
    wingsPersistence.createQuery(Alert.class)
        .filter(Alert.APP_ID_KEY, appId)
        .field("type")
        .in(asList(ApprovalNeeded, ManualInterventionNeeded))
        .filter("status", Open)
        .asList()
        .stream()
        .filter(alert
            -> executionId.equals(alert.getType().equals(ApprovalNeeded)
                    ? ((ApprovalNeededAlert) alert.getAlertData()).getExecutionId()
                    : ((ManualInterventionNeededAlert) alert.getAlertData()).getExecutionId()))
        .forEach(this ::close);
  }

  public Optional<Alert> findExistingAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    if (!alertType.getAlertDataClass().isAssignableFrom(alertData.getClass())) {
      String errorMsg = format("Alert type %s requires alert data of class %s but was %s", alertType.name(),
          alertType.getAlertDataClass().getName(), alertData.getClass().getName());
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", errorMsg);
    }
    injector.injectMembers(alertData);
    Query<Alert> query = wingsPersistence.createQuery(Alert.class).filter("type", alertType).filter("status", Open);
    query = appId == null || appId.equals(GLOBAL_APP_ID) ? query.filter(Alert.ACCOUNT_ID_KEY, accountId)
                                                         : query.filter(Alert.APP_ID_KEY, appId);
    return query.asList()
        .stream()
        .filter(alert -> {
          injector.injectMembers(alert.getAlertData());
          return alertData.matches(alert.getAlertData());
        })
        .findFirst();
  }

  private List<Alert> findExistingAlertsOfType(String accountId, String appId, AlertType alertType) {
    Query<Alert> query = wingsPersistence.createQuery(Alert.class).filter("type", alertType).filter("status", Open);
    query = appId == null || appId.equals(GLOBAL_APP_ID) ? query.filter(Alert.ACCOUNT_ID_KEY, accountId)
                                                         : query.filter(Alert.APP_ID_KEY, appId);
    return query.asList();
  }

  private void close(Alert alert) {
    UpdateOperations<Alert> alertUpdateOperations = wingsPersistence.createUpdateOperations(Alert.class);
    alertUpdateOperations.set(Alert.STATUS_KEY, Closed);
    alertUpdateOperations.set(Alert.CLOSED_AT_KEY, System.currentTimeMillis());
    alertUpdateOperations.set(Alert.VALID_UNTIL_KEY, Date.from(OffsetDateTime.now().plusDays(7).toInstant()));
    wingsPersistence.update(wingsPersistence.createQuery(Alert.class)
                                .filter(Alert.ACCOUNT_ID_KEY, alert.getAccountId())
                                .filter(ID_KEY, alert.getUuid()),
        alertUpdateOperations);
    logger.info("Alert closed: {}", alert);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).filter(Alert.ACCOUNT_ID_KEY, accountId).asList();
    alerts.forEach(alert -> wingsPersistence.delete(alert));
  }

  @Override
  public void pruneByApplication(String appId) {
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).filter(Alert.APP_ID_KEY, appId).asList();
    alerts.forEach(alert -> wingsPersistence.delete(alert));
  }
}
