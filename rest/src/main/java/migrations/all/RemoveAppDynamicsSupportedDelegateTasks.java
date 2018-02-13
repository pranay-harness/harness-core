package migrations.all;

import com.google.inject.Inject;

import migrations.Migration;
import migrations.MigrationUtil;
import software.wings.dl.WingsPersistence;

/**
 * Created by rsingh on 2/12/18.
 */
public class RemoveAppDynamicsSupportedDelegateTasks implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    MigrationUtil.removeDelegateTaskType("APPDYNAMICS_GET_BUSINESS_TRANSACTION_TASK", wingsPersistence);
    MigrationUtil.removeDelegateTaskType("APPDYNAMICS_GET_NODES_TASK", wingsPersistence);
    MigrationUtil.removeDelegateTaskType("APPDYNAMICS_GET_METRICES_OF_BT", wingsPersistence);
    MigrationUtil.removeDelegateTaskType("APPDYNAMICS_GET_METRICES_DATA", wingsPersistence);
  }
}
