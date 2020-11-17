package io.harness.cvng.activity.services.api;

import io.harness.cvng.activity.beans.KubernetesActivitySourceDTO;
import io.harness.cvng.activity.entities.KubernetesActivitySource;
import io.harness.cvng.beans.KubernetesActivityDTO;

import java.util.List;
import javax.validation.constraints.NotNull;

public interface KubernetesActivitySourceService {
  KubernetesActivitySource getActivitySource(@NotNull String activitySourceId);
  String saveKubernetesSource(
      String accountId, String orgIdentifier, String projectIdentifier, KubernetesActivitySourceDTO activitySourceDTO);
  List<String> saveKubernetesSources(String accountId, String orgIdentifier, String projectIdentifier,
      List<KubernetesActivitySourceDTO> activitySourceDTOs);
  boolean saveKubernetesActivities(String accountId, String activitySourceId, List<KubernetesActivityDTO> activities);
  void enqueueDataCollectionTask(KubernetesActivitySource activitySource);
  boolean doesAActivitySourceExistsForThisProject(String accountId, String orgIdentifier, String projectIdentifier);
  int getNumberOfServicesSetup(String accountId, String orgIdentifier, String projectIdentifier);
  List<String> getKubernetesNamespaces(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier);
  List<String> getKubernetesWorkloads(
      String accountId, String orgIdentifier, String projectIdentifier, String connectorIdentifier, String namespace);
}
