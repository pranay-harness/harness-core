/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
