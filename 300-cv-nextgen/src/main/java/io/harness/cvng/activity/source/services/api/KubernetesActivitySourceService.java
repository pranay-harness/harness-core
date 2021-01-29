package io.harness.cvng.activity.source.services.api;

import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.encryption.Scope;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import javax.annotation.Nullable;

public interface KubernetesActivitySourceService {
  boolean saveKubernetesActivities(String accountId, String activitySourceId, List<KubernetesActivityDTO> activities);
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
}
