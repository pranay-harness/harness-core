package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class K8sDeploymentRollingSetupRequest extends K8sCommandRequest {
  StoreType manifestStoreTypes;
  List<ManifestFile> manifestFiles;
  @Builder
  public K8sDeploymentRollingSetupRequest(String accountId, String appId, String commandName, String activityId,
      K8sCommandType k8sCommandType, K8sClusterConfig k8sClusterConfig, String workflowExecutionId,
      String infraMappingId, Integer timeoutIntervalInMin, StoreType manifestStoreTypes,
      List<ManifestFile> manifestFiles) {
    super(accountId, appId, commandName, activityId, k8sClusterConfig, workflowExecutionId, infraMappingId,
        timeoutIntervalInMin, k8sCommandType);
    this.manifestStoreTypes = manifestStoreTypes;
    this.manifestFiles = manifestFiles;
  }
}
