package software.wings.logcontext;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;

import software.wings.beans.alert.AlertType;
import software.wings.service.impl.AppLogContext;

public class AlertLogContext extends AutoLogContext {
  public static final String ALERT_TYPE = "alertType";

  public AlertLogContext(String accountId, AlertType alertType, String appId, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put(AccountLogContext.ID, accountId)
              .putIfNotNull(ALERT_TYPE, alertType.toString())
              .putIfNotNull(AppLogContext.ID, appId)
              .build(),
        behavior);
  }
}
