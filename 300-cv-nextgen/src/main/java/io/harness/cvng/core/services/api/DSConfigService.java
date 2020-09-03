package io.harness.cvng.core.services.api;

import io.harness.cvng.core.beans.DSConfig;

import java.util.List;

public interface DSConfigService {
  List<DSConfig> list(String accountId, String connectorIdentifier, String productName);
  void upsert(DSConfig dsConfig);
  void delete(String accountId, String connectorIdentifier, String productName, String identifier);
}
