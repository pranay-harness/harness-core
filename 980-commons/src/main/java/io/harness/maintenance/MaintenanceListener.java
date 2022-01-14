package io.harness.maintenance;

/**
 * Created by brett on 9/15/17
 */
public interface MaintenanceListener {
  void onShutdown();

  void onEnterMaintenance();

  void onLeaveMaintenance();
}
