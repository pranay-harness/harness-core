/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.ccm.cluster.entities.ClusterType.DIRECT_KUBERNETES;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;

import software.wings.graphql.schema.type.QLCluster.QLClusterBuilder;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class ClusterController {
  private static String DEFAULT = "Cluster type not ECS/Direct Kubernetes";

  private ClusterController() {}

  public static void populateCluster(Cluster cluster, String clusterId, QLClusterBuilder builder) {
    String clusterName = DEFAULT;
    String cloudProviderId = DEFAULT;
    String clusterType = DEFAULT;
    if (cluster.getClusterType().equals(AWS_ECS)) {
      EcsCluster ecsCluster = (EcsCluster) cluster;
      clusterName = ecsCluster.getClusterName();
      cloudProviderId = ecsCluster.getCloudProviderId();
      clusterType = ecsCluster.getClusterType();
    } else if (cluster.getClusterType().equals(DIRECT_KUBERNETES)) {
      DirectKubernetesCluster kubernetesCluster = (DirectKubernetesCluster) cluster;
      clusterName = kubernetesCluster.getClusterName();
      cloudProviderId = kubernetesCluster.getCloudProviderId();
      clusterType = kubernetesCluster.getClusterType();
    }
    builder.id(clusterId).cloudProviderId(cloudProviderId).name(clusterName).clusterType(clusterType);
  }
}
