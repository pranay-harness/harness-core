package software.wings.service.intfc;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

import java.util.concurrent.Future;

public interface AlertService extends OwnedByAccount, OwnedByApplication {
  PageResponse<Alert> list(PageRequest<Alert> pageRequest);

  Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void activeDelegateUpdated(String accountId, String delegateId);

  void deploymentCompleted(String appId, String executionId);

  void deleteByAccountId(String accountId);

  void deleteOldAlerts(long retentionMillis);
}
