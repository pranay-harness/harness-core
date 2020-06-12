package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DSConfig;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorId, String productName);
  void upsert(DSConfig dsConfig);
  void delete(String accountId, String connectorId, String productName, String identifier);
}
