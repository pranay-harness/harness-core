package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class AppLogContext extends AutoLogContext {
  public AppLogContext(String appId) {
    super("appId", appId);
  }
}
