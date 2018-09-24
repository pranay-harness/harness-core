package software.wings.core.maintenance;

/**
 * Created by brett on 9/15/17
 */
public interface MaintenanceListener {
  void onEnterMaintenance();

  void onLeaveMaintenance();
}
