package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.TEN_MINUTES;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.alerts.AlertStatus.Closed;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.alerts.AlertType.ApprovalNeeded;
import static software.wings.alerts.AlertType.ManualInterventionNeeded;
import static software.wings.alerts.AlertType.NoActiveDelegates;
import static software.wings.alerts.AlertType.NoEligibleDelegates;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.alert.Alert.AlertBuilder.anAlert;

import com.google.inject.Injector;

import com.mongodb.BasicDBObject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.alerts.AlertType;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.ApprovalAlert;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;

public class AlertServiceImpl implements AlertService {
  private static final Logger logger = LoggerFactory.getLogger(AlertServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private Injector injector;

  @Override
  public PageResponse<Alert> list(PageRequest<Alert> pageRequest) {
    return wingsPersistence.query(Alert.class, pageRequest);
  }

  @Override
  public void openAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    executorService.submit(() -> openInternal(accountId, appId, alertType, alertData));
  }

  @Override
  public void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    executorService.submit(() -> findExistingAlert(accountId, appId, alertType, alertData).ifPresent(this ::close));
  }

  @Override
  public void activeDelegateUpdated(String accountId, String delegateId) {
    executorService.submit(() -> activeDelegateUpdatedInternal(accountId, delegateId));
  }

  @Override
  public void deploymentCompleted(String appId, String executionId) {
    executorService.submit(() -> deploymentAbortedInternal(appId, executionId));
  }

  private void deploymentAbortedInternal(String appId, String executionId) {
    wingsPersistence.createQuery(Alert.class)
        .field("appId")
        .equal(appId)
        .field("type")
        .in(asList(ApprovalNeeded, ManualInterventionNeeded))
        .field("status")
        .equal(Open)
        .asList()
        .stream()
        .filter(alert
            -> executionId.equals(alert.getType().equals(ApprovalNeeded)
                    ? ((ApprovalAlert) alert.getAlertData()).getExecutionId()
                    : ((ManualInterventionNeededAlert) alert.getAlertData()).getExecutionId()))
        .forEach(this ::close);
  }

  private void openInternal(String accountId, String appId, AlertType alertType, AlertData alertData) {
    if (!findExistingAlert(accountId, appId, alertType, alertData).isPresent()) {
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
      logger.warn("Alert opened: {}", persistedAlert);
    }
  }

  private void activeDelegateUpdatedInternal(String accountId, String delegateId) {
    findExistingAlert(
        accountId, GLOBAL_APP_ID, NoActiveDelegates, NoActiveDelegatesAlert.builder().accountId(accountId).build())
        .ifPresent(this ::close);
    wingsPersistence.createQuery(Alert.class)
        .field("accountId")
        .equal(accountId)
        .field("type")
        .equal(NoEligibleDelegates)
        .field("status")
        .equal(Open)
        .asList()
        .stream()
        .filter(alert
            -> assignDelegateService.canAssign(((NoEligibleDelegatesAlert) alert.getAlertData()).getTask(), delegateId))
        .forEach(this ::close);
  }

  private Optional<Alert> findExistingAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    injector.injectMembers(alertData);
    Query<Alert> query =
        wingsPersistence.createQuery(Alert.class).field("type").equal(alertType).field("status").equal(Open);
    query = appId == null || appId.equals(GLOBAL_APP_ID) ? query.field("accountId").equal(accountId)
                                                         : query.field("appId").equal(appId);
    return query.asList()
        .stream()
        .filter(alert -> {
          injector.injectMembers(alert.getAlertData());
          return alertData.matches(alert.getAlertData());
        })
        .findFirst();
  }

  private void close(Alert alert) {
    UpdateOperations<Alert> alertUpdateOperations = wingsPersistence.createUpdateOperations(Alert.class);
    alertUpdateOperations.set("status", Closed);
    alertUpdateOperations.set("closedAt", System.currentTimeMillis());
    wingsPersistence.update(wingsPersistence.createQuery(Alert.class)
                                .field("accountId")
                                .equal(alert.getAccountId())
                                .field(ID_KEY)
                                .equal(alert.getUuid()),
        alertUpdateOperations);
    logger.info("Alert closed: {}", alert);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).field("accountId").equal(accountId).asList();
    alerts.forEach(alert -> wingsPersistence.delete(alert));
  }

  @Override
  public void deleteByApp(String appId) {
    List<Alert> alerts = wingsPersistence.createQuery(Alert.class).field("appId").equal(appId).asList();
    alerts.forEach(alert -> wingsPersistence.delete(alert));
  }

  @Override
  public void deleteOldAlerts(long retentionMillis) {
    final int batchSize = 1000;
    final int limit = 5000;
    final long days = retentionMillis / (24 * 60 * 60 * 1000L);
    try {
      logger.info("Start: Deleting alerts older than {} days", days);
      with().pollInterval(2L, TimeUnit.SECONDS).await().atMost(TEN_MINUTES).until(() -> {
        List<Alert> alerts = wingsPersistence.createQuery(Alert.class)
                                 .limit(limit)
                                 .batchSize(batchSize)
                                 .field("status")
                                 .equal(Closed)
                                 .field("createdAt")
                                 .lessThan(System.currentTimeMillis() - retentionMillis)
                                 .asList();
        if (isEmpty(alerts)) {
          logger.info("No more alerts older than {} days", days);
          return true;
        }
        try {
          logger.info("Deleting {} alerts", alerts.size());
          List<String> alertIds = alerts.stream().map(Alert::getUuid).collect(Collectors.toList());
          wingsPersistence.getCollection("alerts").remove(
              new BasicDBObject("_id", new BasicDBObject("$in", alertIds.toArray())));
        } catch (Exception ex) {
          logger.warn("Failed to delete {} alerts", alerts.size(), ex);
        }
        logger.info("Successfully deleted {} alerts", alerts.size());
        return alerts.size() < limit;
      });
    } catch (Exception ex) {
      logger.warn("Failed to delete alerts older than {} days within 10 minutes.", days, ex);
    }
    logger.info("Deleted alerts older than {} days", days);
  }
}
