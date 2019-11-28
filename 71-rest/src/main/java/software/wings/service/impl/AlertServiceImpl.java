package software.wings.service.impl;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static software.wings.alerts.AlertStatus.Closed;
import static software.wings.alerts.AlertStatus.Open;
import static software.wings.alerts.AlertStatus.Pending;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.alert.AlertType.ApprovalNeeded;
import static software.wings.beans.alert.AlertType.CONTINUOUS_VERIFICATION_ALERT;
import static software.wings.beans.alert.AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT;
import static software.wings.beans.alert.AlertType.DelegateProfileError;
import static software.wings.beans.alert.AlertType.DelegatesDown;
import static software.wings.beans.alert.AlertType.GitConnectionError;
import static software.wings.beans.alert.AlertType.GitSyncError;
import static software.wings.beans.alert.AlertType.INSTANCE_USAGE_APPROACHING_LIMIT;
import static software.wings.beans.alert.AlertType.InvalidKMS;
import static software.wings.beans.alert.AlertType.ManualInterventionNeeded;
import static software.wings.beans.alert.AlertType.NoActiveDelegates;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;
import static software.wings.beans.alert.AlertType.RESOURCE_USAGE_APPROACHING_LIMIT;
import static software.wings.beans.alert.AlertType.USAGE_LIMIT_EXCEEDED;
import static software.wings.beans.alert.AlertType.USERGROUP_SYNC_FAILED;
import static software.wings.utils.Misc.getDurationString;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.eraro.ErrorCode;
import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.event.publisher.EventPublisher;
import io.harness.exception.WingsException;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.alerts.AlertStatus;
import software.wings.beans.SettingAttribute;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.alert.ManualInterventionNeededAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.logcontext.AlertLogContext;
import software.wings.service.impl.event.AlertEvent;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.alert.NotificationRulesStatusService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Singleton
@Slf4j
public class AlertServiceImpl implements AlertService {
  // TODO: check if ARTIFACT_COLLECTION_FAILED alert type needs to be added here
  private static final List<AlertType> ALERT_TYPES_TO_NOTIFY_ON = ImmutableList.of(NoActiveDelegates, DelegatesDown,
      NoEligibleDelegates, DelegateProfileError, DEPLOYMENT_RATE_APPROACHING_LIMIT, INSTANCE_USAGE_APPROACHING_LIMIT,
      USAGE_LIMIT_EXCEEDED, USERGROUP_SYNC_FAILED, RESOURCE_USAGE_APPROACHING_LIMIT, GitSyncError, GitConnectionError,
      InvalidKMS, CONTINUOUS_VERIFICATION_ALERT);
  private static final List<AlertType> CLOSED_ALERT_TYPES_TO_NOTIFY_ON =
      ImmutableList.of(CONTINUOUS_VERIFICATION_ALERT, InvalidKMS);
  private static final Iterable<AlertStatus> STATUS_ACTIVE = ImmutableSet.of(Open, Pending);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutorService executorService;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private Injector injector;
  @Inject private EventPublisher eventPublisher;
  @Inject private NotificationRulesStatusService notificationStatusService;
  @Inject private AppService appService;
  @Inject private SettingsService settingsService;
  @Inject private ArtifactStreamService artifactStreamService;

  @Override
  public PageResponse<Alert> list(PageRequest<Alert> pageRequest) {
    return wingsPersistence.query(Alert.class, pageRequest);
  }

  @Override
  public List<AlertType> listCategoriesAndTypes(String accountId) {
    return Arrays.stream(software.wings.beans.alert.AlertType.values())
        .filter(AlertServiceImpl.ALERT_TYPES_TO_NOTIFY_ON::contains)
        .collect(toList());
  }

  @Override
  public Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    return executorService.submit(() -> openInternal(accountId, appId, alertType, alertData));
  }

  @Override
  public void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    executorService.submit(() -> findExistingAlert(accountId, appId, alertType, alertData).ifPresent(this ::close));
  }

  @Override
  public void closeAllAlerts(String accountId, String appId, AlertType alertType, AlertData alertData) {
    executorService.submit(() -> findAllExistingAlerts(accountId, appId, alertType, alertData).forEach(this ::close));
  }

  @Override
  public void closeAlertsOfType(String accountId, String appId, AlertType alertType) {
    executorService.submit(() -> findExistingAlertsOfType(accountId, appId, alertType).forEach(this ::close));
  }

  @Override
  public void activeDelegateUpdated(String accountId, String delegateId) {
    executorService.submit(() -> activeDelegateUpdatedInternal(accountId, delegateId));
  }

  @Override
  public void deploymentCompleted(String appId, String executionId) {
    executorService.submit(() -> deploymentCompletedInternal(appId, executionId));
  }

  private void openInternal(String accountId, String appId, AlertType alertType, AlertData alertData) {
    try (AutoLogContext ignore = new AlertLogContext(accountId, alertType, appId, OVERRIDE_ERROR)) {
      Alert alert = findExistingAlert(accountId, appId, alertType, alertData).orElse(null);
      if (alert == null) {
        injector.injectMembers(alertData);
        alert = Alert.builder()
                    .appId(appId)
                    .accountId(accountId)
                    .type(alertType)
                    .status(Pending)
                    .alertData(alertData)
                    .title(alertData.buildTitle())
                    .resolutionTitle(alertData.buildResolutionTitle())
                    .category(alertType.getCategory())
                    .severity(alertType.getSeverity())
                    .triggerCount(0)
                    .build();
        wingsPersistence.save(alert);
        logger.info("Alert created: {}", alert);
      }
      AlertStatus status = alert.getTriggerCount() >= alertType.getPendingCount() ? Open : Pending;
      boolean alertOpened = false;
      UpdateOperations<Alert> updateOperations =
          wingsPersistence.createUpdateOperations(Alert.class).inc(AlertKeys.triggerCount);
      if (status == Open && alert.getStatus() == Pending) {
        updateOperations.set(AlertKeys.status, Open);
        alertOpened = true;
      }
      wingsPersistence.update(alert, updateOperations);

      alert.setTriggerCount(alert.getTriggerCount() + 1);
      alert.setStatus(status);
      if (alertOpened) {
        logger.info("Alert opened: {}", alert);

        if (notificationStatusService.get(accountId).isEnabled()) {
          publishEvent(alert);
        } else {
          logger.info("No alert event will be published. accountId={}", accountId);
        }
      } else if (status == Pending) {
        logger.info("Alert pending: {}", alert);
      }
    }
  }

  private void publishEvent(Alert alert) {
    try {
      if (ALERT_TYPES_TO_NOTIFY_ON.contains(alert.getType())) {
        eventPublisher.publishEvent(
            Event.builder().eventData(alertEventData(alert)).eventType(EventType.OPEN_ALERT).build());
      } else {
        logger.info("No alert event will be published in event queue. Type: {}", alert.getType());
      }
    } catch (Exception e) {
      logger.error("Could not publish alert event. Alert: {}", alert);
    }
  }

  private void publishCloseEvent(Alert alert) {
    try {
      if (CLOSED_ALERT_TYPES_TO_NOTIFY_ON.contains(alert.getType())) {
        eventPublisher.publishEvent(
            Event.builder().eventData(alertEventData(alert)).eventType(EventType.CLOSE_ALERT).build());
      } else {
        logger.info("No close alert event will be published in event queue. Type: {}", alert.getType());
      }
    } catch (Exception e) {
      logger.error("Could not publish alert event. Alert: {}", alert);
    }
  }

  private static EventData alertEventData(Alert alert) {
    return EventData.builder().eventInfo(new AlertEvent(alert)).build();
  }

  private void activeDelegateUpdatedInternal(String accountId, String delegateId) {
    findExistingAlert(
        accountId, GLOBAL_APP_ID, NoActiveDelegates, NoActiveDelegatesAlert.builder().accountId(accountId).build())
        .ifPresent(this ::close);
    Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class)
                                  .filter(AlertKeys.accountId, accountId)
                                  .filter(AlertKeys.type, NoEligibleDelegates)
                                  .field(AlertKeys.status)
                                  .in(STATUS_ACTIVE);
    for (Alert alert : alertQuery) {
      NoEligibleDelegatesAlert data = (NoEligibleDelegatesAlert) alert.getAlertData();
      if (assignDelegateService.canAssign(delegateId, accountId, data.getAppId(), data.getEnvId(),
              data.getInfraMappingId(), data.getTaskGroup(), data.getTags())) {
        close(alert);
      }
    }
  }

  private void deploymentCompletedInternal(String appId, String executionId) {
    Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class)
                                  .filter(AlertKeys.appId, appId)
                                  .field(AlertKeys.type)
                                  .in(asList(ApprovalNeeded, ManualInterventionNeeded))
                                  .field(AlertKeys.status)
                                  .in(STATUS_ACTIVE);
    for (Alert alert : alertQuery) {
      String alertExecutionId = alert.getType().equals(ApprovalNeeded)
          ? ((ApprovalNeededAlert) alert.getAlertData()).getExecutionId()
          : ((ManualInterventionNeededAlert) alert.getAlertData()).getExecutionId();
      if (executionId.equals(alertExecutionId)) {
        close(alert);
      }
    }
  }

  public Optional<Alert> findExistingAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    try (AutoLogContext ignore = new AlertLogContext(accountId, alertType, appId, OVERRIDE_ERROR)) {
      logger.info("Finding existing alerts for type: {}", alertType);
      Query<Alert> alertQuery = getAlertsQuery(accountId, appId, alertType, alertData);
      for (Alert alert : alertQuery) {
        injector.injectMembers(alert.getAlertData());
        if (alertData.matches(alert.getAlertData())) {
          return Optional.of(alert);
        }
      }
    }
    return Optional.empty();
  }

  private List<Alert> findAllExistingAlerts(String accountId, String appId, AlertType alertType, AlertData alertData) {
    try (AutoLogContext ignore = new AlertLogContext(accountId, alertType, appId, OVERRIDE_ERROR)) {
      logger.info("Finding all existing alerts");
      Query<Alert> alertQuery = getAlertsQuery(accountId, appId, alertType, alertData);
      List<Alert> alerts = new ArrayList<>();
      for (Alert alert : alertQuery) {
        injector.injectMembers(alert.getAlertData());
        if (alertData.matches(alert.getAlertData())) {
          alerts.add(alert);
        }
      }
      return alerts;
    }
  }

  private Query<Alert> getAlertsQuery(String accountId, String appId, AlertType alertType, AlertData alertData) {
    if (!alertType.getAlertDataClass().isAssignableFrom(alertData.getClass())) {
      String errorMsg = format("Alert type %s requires alert data of class %s but was %s", alertType.name(),
          alertType.getAlertDataClass().getName(), alertData.getClass().getName());
      logger.error(errorMsg);
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", errorMsg);
    }
    Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class)
                                  .filter(AlertKeys.accountId, accountId)
                                  .filter(AlertKeys.type, alertType)
                                  .field(AlertKeys.status)
                                  .in(STATUS_ACTIVE);
    if (appId != null) {
      alertQuery.filter(AlertKeys.appId, appId);
    }
    injector.injectMembers(alertData);
    return alertQuery;
  }

  private Iterable<Alert> findExistingAlertsOfType(String accountId, String appId, AlertType alertType) {
    Query<Alert> alertQuery = wingsPersistence.createQuery(Alert.class)
                                  .filter(AlertKeys.accountId, accountId)
                                  .filter(AlertKeys.type, alertType)
                                  .field(AlertKeys.status)
                                  .in(STATUS_ACTIVE);
    if (appId != null) {
      alertQuery.filter(AlertKeys.appId, appId);
    }
    return alertQuery;
  }

  private void close(Alert alert) {
    long now = System.currentTimeMillis();
    Date expiration = Date.from(OffsetDateTime.now().plusDays(5).toInstant());

    final UpdateResults updateResults = wingsPersistence.update(wingsPersistence.createQuery(Alert.class)
                                                                    .filter(AlertKeys.uuid, alert.getUuid())
                                                                    .filter(AlertKeys.accountId, alert.getAccountId()),
        wingsPersistence.createUpdateOperations(Alert.class)
            .set(AlertKeys.status, Closed)
            .set(AlertKeys.closedAt, now)
            .set(AlertKeys.validUntil, expiration));

    if (updateResults.getUpdatedCount() > 0) {
      logger.info("Alert closed after {} : {}", getDurationString(alert.getCreatedAt(), now), alert.getUuid());
      alert.setStatus(Closed);
      alert.setClosedAt(now);
      alert.setValidUntil(expiration);
      publishCloseEvent(alert);
    }
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.createQuery(Alert.class)
        .filter(AlertKeys.accountId, accountId)
        .forEach(alert -> wingsPersistence.delete(alert));
  }

  @Override
  public void pruneByApplication(String appId) {
    wingsPersistence.createQuery(Alert.class)
        .filter(AlertKeys.appId, appId)
        .forEach(alert -> wingsPersistence.delete(alert));
  }

  @Override
  public void pruneByArtifactStream(String appId, String artifactStreamId) {
    // NOTE: this pruning is done only for ArtifactCollectionFailedAlert
    Iterable<Alert> alerts;
    if (GLOBAL_APP_ID.equals(appId)) {
      ArtifactStream artifactStream = artifactStreamService.get(artifactStreamId);
      if (artifactStream == null || artifactStream.getSettingId() == null) {
        return;
      }
      SettingAttribute settingAttribute = settingsService.get(artifactStream.getSettingId());
      if (settingAttribute == null) {
        return;
      }

      // NOTE: appId not known at this point, hence set to null - appId will be removed from this alert type later
      alerts = findExistingAlertsOfType(settingAttribute.getAccountId(), null, AlertType.ARTIFACT_COLLECTION_FAILED);
    } else {
      alerts =
          findExistingAlertsOfType(appService.getAccountIdByAppId(appId), null, AlertType.ARTIFACT_COLLECTION_FAILED);
    }

    for (Alert alert : alerts) {
      ArtifactCollectionFailedAlert data = (ArtifactCollectionFailedAlert) alert.getAlertData();
      if (data.getArtifactStreamId().equals(artifactStreamId)) {
        wingsPersistence.delete(alert);
      }
    }
  }
}
