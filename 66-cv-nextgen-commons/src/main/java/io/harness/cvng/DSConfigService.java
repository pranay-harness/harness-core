package io.harness.cvng;

import io.harness.cvng.models.DSConfig;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorId, String productName);
  void upsert(DSConfig dsConfig);
  void delete(String accountId, String connectorId, String productName, String identifier);
}
