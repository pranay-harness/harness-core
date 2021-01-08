package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.dashboard.beans.EnvToServicesDTO;
import io.harness.encryption.Scope;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

public interface CVConfigService extends DeleteEntityByProjectHandler<CVConfig> {
  CVConfig save(CVConfig cvConfig);
  List<CVConfig> save(List<CVConfig> cvConfig);
  void update(CVConfig cvConfig);
  void update(List<CVConfig> cvConfigs);
  @Nullable CVConfig get(String cvConfigId);
  void delete(String cvConfigId);

  void deleteByIdentifier(
      String accountId, String orgIdentifier, String projectIdentifier, String monitoringSourceIdentifier);
  List<CVConfig> findByConnectorIdentifier(String accountId, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, String connectorIdentifierWithoutScopePrefix, Scope scope);
  List<CVConfig> list(String accountId, String connectorIdentifier);
  List<CVConfig> list(String accountId, String connectorIdentifier, String productName);
  List<CVConfig> list(
      String accountId, String connectorIdentifier, String productName, String monitoringSourceIdentifier);
  List<CVConfig> list(String accountId, String orgIdentifier, String projectIdentifier, String environmentIdentifier,
      String serviceIdentifier, CVMonitoringCategory monitoringCategory);
  List<String> getProductNames(String accountId, String connectorIdentifier);
  List<String> getMonitoringSourceIds(String accountId, String orgIdentifier, String projectIdentifier, String filter);
  List<CVConfig> listByMonitoringSources(
      String accountId, String orgIdentifier, String projectIdentifier, List<String> monitoringSourceIdentifier);

  void setCollectionTaskId(String cvConfigId, String dataCollectionTaskId);
  List<CVConfig> find(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, List<DataSourceType> dataSourceTypes);
  List<EnvToServicesDTO> getEnvToServicesList(String accountId, String orgIdentifier, String projectIdentifier);
  Map<String, Set<String>> getEnvToServicesMap(String accountId, String orgIdentifier, String projectIdentifier);
  Set<CVMonitoringCategory> getAvailableCategories(
      String accountId, String orgIdentifier, String projectIdentifier, String envIdentifier, String serviceIdentifier);
  List<CVConfig> getConfigsOfProductionEnvironments(String accountId, String orgIdentifier, String projectIdentifier,
      String environmentIdentifier, String serviceIdentifier, CVMonitoringCategory monitoringCategory);
  boolean isProductionConfig(CVConfig cvConfig);

  List<CVConfig> getCVConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier);
  boolean doesAnyCVConfigExistsInProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfServicesSetup(String accountId, String orgIdentifier, String projectIdentifier);
  void deleteConfigsForProject(String accountId, String orgIdentifier, String projectIdentifier);
  List<CVConfig> getExistingMappedConfigs(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String identifier);
  Set<DatasourceTypeDTO> getDataSourcetypes(String accountId, String projectIdentifier, String orgIdentifier,
      String environmentIdentifier, String serviceIdentifier, CVMonitoringCategory monitoringCategory);
  List<String> cleanupPerpetualTasks(String accountId, List<String> cvConfigIds);
}
