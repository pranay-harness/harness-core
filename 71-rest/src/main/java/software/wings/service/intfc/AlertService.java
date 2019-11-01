package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.alert.Alert;
import software.wings.beans.alert.AlertData;
import software.wings.beans.alert.AlertType;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedByApplication;
import software.wings.service.intfc.ownership.OwnedByArtifactStream;

import java.util.List;
import java.util.concurrent.Future;
import javax.ws.rs.QueryParam;

public interface AlertService extends OwnedByAccount, OwnedByApplication, OwnedByArtifactStream {
  PageResponse<Alert> list(PageRequest<Alert> pageRequest);

  List<AlertType> listCategoriesAndTypes(@QueryParam("accountId") String accountId);

  Future openAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlert(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAllAlerts(String accountId, String appId, AlertType alertType, AlertData alertData);

  void closeAlertsOfType(String accountId, String appId, AlertType alertType);

  void activeDelegateUpdated(String accountId, String delegateId);

  void deploymentCompleted(String appId, String executionId);
}
