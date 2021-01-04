package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.MonitoringSourceDTO;
import io.harness.ng.beans.PageResponse;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorIdentifier, String productName);
  void upsert(DSConfig dsConfig);
  void delete(String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier);
  DSConfig getMonitoringSource(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  PageResponse<MonitoringSourceDTO> listMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, int limit, int offset, String filter);
}
