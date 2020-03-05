package io.harness.service;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.service.intfc.AlertService;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

public class NoOpAlertService implements AlertService {
  @Override
  public PageResponse<Alert> list(PageRequest<Alert> pageRequest) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<AlertType> listCategoriesAndTypes(String accountId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future openAlertWithTTL(
      String accountId, String appId, AlertType alertType, AlertData alertData, Date validUntil) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeAllAlerts(String accountId, String appId, AlertType alertType, AlertData alertData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void closeAlertsOfType(String accountId, String appId, AlertType alertType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void activeDelegateUpdated(String accountId, String delegateId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deploymentCompleted(String appId, String executionId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Alert> findExistingAlert(String accountId, String appId, AlertType alertType, AlertData alertData) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void pruneByApplication(String appId) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteByArtifactStream(String appId, String artifactStreamId) {
    throw new UnsupportedOperationException();
  }
}
