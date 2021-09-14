/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.service.intf;

import io.harness.ccm.remote.beans.K8sClusterSetupRequest;

import java.io.File;
import java.io.IOException;
import lombok.NonNull;

public interface CEYamlService {
  String DOT_YAML = ".yaml";
  String CLOUD_COST_K8S_CLUSTER_SETUP = "cloudCostK8sClusterSetup";

  @Deprecated // use unifiedCloudCostK8sClusterYaml
  File downloadCostOptimisationYaml(String accountId, String connectorIdentifier, String harnessHost, String serverName)
      throws IOException;

  String unifiedCloudCostK8sClusterYaml(@NonNull String accountId, String harnessHost, String serverName,
      @NonNull K8sClusterSetupRequest k8sClusterSetupRequest) throws IOException;
}
