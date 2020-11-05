package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public interface CVConfigService {
  CVConfig save(CVConfig cvConfig);
  List<CVConfig> save(List<CVConfig> cvConfig);
  void update(CVConfig cvConfig);
  void update(List<CVConfig> cvConfigs);
  @Nullable CVConfig get(String cvConfigId);
  void delete(String cvConfigId);

  void deleteByGroupId(String accountId, String connectorIdentifier, String productName, String groupId);
  List<CVConfig> list(String accountId, String connectorIdentifier);
  List<CVConfig> list(String accountId, String connectorIdentifier, String productName);
  List<CVConfig> list(String accountId, String connectorIdentifier, String productName, String groupId);
  List<CVConfig> list(String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier,
      String serviceIdentifier, CVMonitoringCategory monitoringCategory);
  List<String> getProductNames(String accountId, String connectorIdentifier);

  void setCollectionTaskId(String cvConfigId, String dataCollectionTaskId);
  List<CVConfig> find(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, List<DataSourceType> dataSourceTypes);
  List<EnvToServicesDTO> getEnvToServicesList(String accountId, String orgIdentifier, String projectIdentifier);
  Map<String, Set<String>> getEnvToServicesMap(String accountId, String orgIdentifier, String projectIdentifier);
  Set<CVMonitoringCategory> getAvailableCategories(String accountId, String orgIdentifier, String projectIdentifier);
  List<CVConfig> getConfigsOfProductionEnvironments(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, String serviceIdentifier, CVMonitoringCategory monitoringCategory);
  boolean isProductionConfig(CVConfig cvConfig);

  List<CVConfig> getCVConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier);
}
