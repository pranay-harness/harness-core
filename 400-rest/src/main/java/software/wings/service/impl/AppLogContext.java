package software.wings.service.impl;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.LogKeyUtils;

import software.wings.beans.Application;

public class AppLogContext extends AutoLogContext {
  public static final String ID = LogKeyUtils.logKeyForId(Application.class);

  public AppLogContext(String appId, OverrideBehavior behavior) {
    super(ID, appId, behavior);
  }
}
