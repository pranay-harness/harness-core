package io.harness.maintenance;

import java.io.Closeable;
import java.io.IOException;

public class MaintenanceGuard implements Closeable {
  private boolean old;
  public MaintenanceGuard(boolean maintenance) {
    old = MaintenanceController.isMaintenance();
    MaintenanceController.forceMaintenance(maintenance);
  }

  @Override
  public void close() throws IOException {
    MaintenanceController.forceMaintenance(old);
  }
}
