package io.harness.cvng.core.handler.monitoredService;

import io.harness.cvng.core.beans.monitoredService.MonitoredServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;

public abstract class BaseMonitoredServiceHandler {
  public void beforeUpdate(
      ProjectParams projectParams, MonitoredServiceDTO existingObject, MonitoredServiceDTO updatingObject) {}
}
