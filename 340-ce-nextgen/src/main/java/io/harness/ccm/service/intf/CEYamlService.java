package io.harness.ccm.service.intf;

import io.harness.ccm.remote.beans.K8sClusterSetupRequest;

import java.io.File;
import java.io.IOException;
import lombok.NonNull;

public interface CEYamlService {
  String DOT_YAML = ".yaml";
  String CLOUD_COST_K8S_CLUSTER_SETUP = "cloudCostK8sClusterSetup";
  String DOWNLOAD_YAML_FILENAME = "cloud_cost_k8s_cluster_setup";

  // use unifiedCloudCostK8sClusterYaml
  @Deprecated
  File downloadCostOptimisationYaml(String accountId, String connectorIdentifier, String harnessHost, String serverName)
      throws IOException;

  String unifiedCloudCostK8sClusterYaml(@NonNull String accountId, String harnessHost, String serverName,
      @NonNull K8sClusterSetupRequest k8sClusterSetupRequest) throws IOException;
}
