package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.MetricPackDTO;
import io.harness.cvng.core.beans.MetricPackValidationResponse;
import io.harness.cvng.core.beans.dynatrace.DynatraceServiceDTO;
import io.harness.cvng.core.beans.params.ProjectParams;

import java.util.List;
import java.util.Set;

public interface DynatraceService extends DataSourceConnectivityChecker {
  List<DynatraceServiceDTO> getAllServices(
      ProjectParams projectParams, String connectorIdentifier, String filter, String tracingId);

  DynatraceServiceDTO getServiceDetails(
      ProjectParams projectParams, String connectorIdentifier, String serviceEntityId, String tracingId);

  Set<MetricPackValidationResponse> validateData(ProjectParams projectParams, String projectIdentifier,
      String serviceId, List<MetricPackDTO> metricPacks, String tracingId);
}
