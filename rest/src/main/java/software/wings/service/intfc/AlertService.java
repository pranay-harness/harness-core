package software.wings.service.intfc;

import software.wings.alerts.AlertType;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;

public interface AlertService {
  PageResponse<Alert> list(PageRequest<Alert> pageRequest);

  void openAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void activeDelegateUpdated(String accountId, String delegateId);

  void deleteByAccountId(String accountId);

  void deleteByApp(String appId);

  void deleteOldAlerts(long retentionMillis);
}
