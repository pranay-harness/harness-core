package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.DirectKubernetesInfrastructureMapping;

public class ClusterRecordUtilsTest extends CategoryTest {
  private String accoundId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String computeProvider = "clusterName";

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testFrom() {
    DirectKubernetesInfrastructureMapping k8sInfraMapping = DirectKubernetesInfrastructureMapping.builder()
                                                                .accountId(accoundId)
                                                                .infraMappingType(DIRECT_KUBERNETES)
                                                                .cloudProviderId(cloudProviderId)
                                                                .build();
    k8sInfraMapping.setComputeProviderName(computeProvider);

    ClusterRecord expectedClusterRecord =
        ClusterRecord.builder()
            .accountId(accoundId)
            .cluster(
                DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).clusterName(computeProvider).build())
            .build();
    ClusterRecord actualClusterRecord = ClusterRecordUtils.from(k8sInfraMapping);
    assertThat(actualClusterRecord).isEqualTo(expectedClusterRecord);
  }
}
