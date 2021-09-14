/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.maintenance;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public class MaintenanceGuard implements AutoCloseable {
  private boolean old;
  public MaintenanceGuard(boolean maintenance) {
    old = MaintenanceController.getMaintenanceFlag();
    MaintenanceController.forceMaintenance(maintenance);
  }

  @Override
  public void close() {
    MaintenanceController.forceMaintenance(old);
  }
}
