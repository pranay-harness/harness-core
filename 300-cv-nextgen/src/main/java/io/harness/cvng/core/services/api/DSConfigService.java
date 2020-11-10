package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.MonitoringSourceDTO;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorIdentifier, String productName);
  void upsert(DSConfig dsConfig);
  void delete(String accountId, String connectorIdentifier, String productName, String identifier);
  List<MonitoringSourceDTO> listMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, int limit, int offset);
}
