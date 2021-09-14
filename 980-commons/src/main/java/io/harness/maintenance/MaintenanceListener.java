/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.maintenance;

/**
 * Created by brett on 9/15/17
 */
public interface MaintenanceListener {
  void onShutdown();

  void onEnterMaintenance();

  void onLeaveMaintenance();
}
