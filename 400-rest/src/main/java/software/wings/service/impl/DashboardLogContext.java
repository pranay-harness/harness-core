package software.wings.service.impl;

import io.harness.logging.AutoLogContext;

public class DashboardLogContext extends AutoLogContext {
  public static final String ID = "dashboardId";

  public DashboardLogContext(String dashboardId, OverrideBehavior behavior) {
    super(ID, dashboardId, behavior);
  }
}
