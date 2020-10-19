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
  boolean saveKubernetesActivities(String accountId, String activitySourceId, List<KubernetesActivityDTO> activities);
  void enqueueDataCollectionTask(KubernetesActivitySource activitySource);
}
