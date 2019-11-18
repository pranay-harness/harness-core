package software.wings.logcontext;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.logging.AutoLogContext;
import software.wings.beans.alert.AlertType;

public class AlertLogContext extends AutoLogContext {
  public AlertLogContext(String accountId, AlertType alertType, String appId, OverrideBehavior behavior) {
    super(ImmutableMap.of("accountId", accountId, "alertType", alertType.toString(), "appId",
              appId == null ? GLOBAL_APP_ID : appId),
        behavior);
  }
}
