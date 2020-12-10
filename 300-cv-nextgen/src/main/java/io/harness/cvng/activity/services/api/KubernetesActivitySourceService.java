package io.harness.cvng.activity.services.api;

import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.activity.KubernetesActivityDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.ng.beans.PageResponse;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface KubernetesActivitySourceService {
  KubernetesActivitySource getActivitySource(@NotNull String activitySourceId);
  String saveKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, KubernetesActivitySourceDTO activitySourceDTO);
  KubernetesActivitySourceDTO getKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  PageResponse<KubernetesActivitySourceDTO> listKubernetesSources(
      String accountId, String orgIdentifier, String projectIdentifier, int offset, int pageSize, String filter);
  boolean deleteKubernetesSource(String accountId, String orgIdentifier, String projectIdentifier, String identifier);
  boolean saveKubernetesActivities(String accountId, String activitySourceId, List<KubernetesActivityDTO> activities);
  void enqueueDataCollectionTask(KubernetesActivitySource activitySource);
  boolean doesAActivitySourceExistsForThisProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfServicesSetup(String accountId, String orgIdentifier, String projectIdentifier);
  PageResponse<String> getKubernetesNamespaces(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, int offset, int pageSize, String filter);

  PageResponse<String> getKubernetesWorkloads(String accountId, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier, String namespace, int offset, int pageSize, String filter);
}
