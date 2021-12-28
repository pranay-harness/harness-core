package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.dynatrace.DynatraceServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;

import javax.validation.constraints.NotNull;
import java.util.List;

public interface DynatraceService extends DataSourceConnectivityChecker {
  List<DynatraceServiceDTO> getAllServices(ProjectParams projectParams, String connectorIdentifier, String filter, String tracingId);

  DynatraceServiceDTO getServiceDetails(
      ProjectParams projectParams, String connectorIdentifier, String serviceEntityId, String tracingId);
}
