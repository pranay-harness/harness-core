package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedByApplication;

import java.util.Optional;
import java.util.concurrent.Future;

public interface AlertService extends OwnedByAccount, OwnedByApplication {
  PageResponse<Alert> list(PageRequest<Alert> pageRequest);

  Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlertsOfType(String accountId, String appId, AlertType alertType);

  void activeDelegateUpdated(String accountId, String delegateId);

  void deploymentCompleted(String appId, String executionId);

  Optional<Alert> findExistingAlert(String accountId, String appId, AlertType alertType, AlertData alertData);
}
