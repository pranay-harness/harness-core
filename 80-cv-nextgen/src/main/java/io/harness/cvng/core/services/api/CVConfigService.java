package io.harness.cvng.core.services.api;

import io.harness.cvng.core.entities.CVConfig;

import java.util.List;
import javax.annotation.Nullable;

public interface CVConfigService {
  CVConfig save(CVConfig cvConfig);
  List<CVConfig> save(List<CVConfig> cvConfig);
  void update(CVConfig cvConfig);
  void update(List<CVConfig> cvConfigs);
  @Nullable CVConfig get(String cvConfigId);
  void delete(String cvConfigId);
  void deleteByGroupId(String accountId, String connectorId, String productName, String groupId);
  List<CVConfig> list(String accountId, String connectorId);
  List<CVConfig> list(String accountId, String connectorId, String productName);
  List<CVConfig> list(String accountId, String connectorId, String productName, String groupId);
  List<String> getProductNames(String accountId, String connectorId);

  void setCollectionTaskId(String uuid, String dataCollectionTaskId);
}
