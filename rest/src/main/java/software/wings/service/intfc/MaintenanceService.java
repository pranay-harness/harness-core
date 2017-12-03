package software.wings.service.intfc;

import io.dropwizard.lifecycle.Managed;
import software.wings.core.maintenance.MaintenanceListener;

public interface MaintenanceService extends Managed {
  boolean isMaintenance();

  void register(MaintenanceListener listener);
}
