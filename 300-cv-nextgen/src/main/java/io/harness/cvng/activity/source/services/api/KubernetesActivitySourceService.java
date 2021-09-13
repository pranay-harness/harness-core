package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.activity.beans.KubernetesActivityDetailsDTO;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.change.KubernetesChangeEventDTO;
import io.harness.cvng.core.services.api.DataSourceConnectivityChecker;
import io.harness.encryption.Scope;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import javax.annotation.Nullable;

public interface KubernetesActivitySourceService extends DataSourceConnectivityChecker {
  boolean saveKubernetesActivities(String accountId, String activitySourceId, List<KubernetesActivityDTO> activities);
  boolean saveKubernetesChange(String accountId, KubernetesChangeEventDTO changeEventDTO);
  void enqueueDataCollectionTask(KubernetesActivitySource activitySource);
  boolean doesAActivitySourceExistsForThisProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfKubernetesServicesSetup(String accountId, String orgIdentifier, String projectIdentifier);
  PageResponse<String> getKubernetesNamespaces(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, int offset, int pageSize, String filter);
  PageResponse<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, int offset, int pageSize, String filter);

  void resetLiveMonitoringPerpetualTaskForKubernetesActivitySource(KubernetesActivitySource kubernetesActivitySource);

  List<KubernetesActivitySource> findByConnectorIdentifier(String accountId, @Nullable String orgIdentifier,
      @Nullable String projectIdentifier, String connectorIdentifierWithoutScopePrefix, Scope scope);

  KubernetesActivityDetailsDTO getEventDetails(
      String accountId, String orgIdentifier, String projectIdentifier, String activityId);
}
