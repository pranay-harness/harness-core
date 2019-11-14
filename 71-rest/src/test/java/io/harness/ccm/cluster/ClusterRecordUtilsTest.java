package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.UNKNOWN;
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

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testFrom() {
    DirectKubernetesInfrastructureMapping k8sInfraMapping = DirectKubernetesInfrastructureMapping.builder()
                                                                .accountId(accoundId)
                                                                .infraMappingType(DIRECT_KUBERNETES)
                                                                .cloudProviderId(cloudProviderId)
                                                                .build();

    ClusterRecord expectedClusterRecord =
        ClusterRecord.builder()
            .accountId(accoundId)
            .cluster(DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build())
            .build();
    ClusterRecord actualClusterRecord = ClusterRecordUtils.from(k8sInfraMapping);
    assertThat(actualClusterRecord).isEqualTo(expectedClusterRecord);
  }
}
