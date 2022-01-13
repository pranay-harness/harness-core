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
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLClusterQueryParameters;
import software.wings.graphql.schema.type.QLCluster;
import software.wings.security.UserThreadLocal;

import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class ClusterDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock ClusterRecordService clusterRecordService;
  @InjectMocks ClusterDataFetcher clusterDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterDataFetcherWhenClusterIdIsNull() {
    assertThatThrownBy(
        () -> clusterDataFetcher.fetch(QLClusterQueryParameters.builder().clusterId(null).build(), ACCOUNT1_ID))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterDataFetcherEcs() {
    when(clusterRecordService.get(CLUSTER2_ID))
        .thenReturn(
            mockEcsClusterRecord(ACCOUNT1_ID, CLUSTER1_NAME, CLUSTER2_ID, CLOUD_PROVIDER1_ID_ACCOUNT1, REGION1));
    QLCluster qlCluster =
        clusterDataFetcher.fetch(QLClusterQueryParameters.builder().clusterId(CLUSTER2_ID).build(), ACCOUNT1_ID);

    assertThat(qlCluster.getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(qlCluster.getId()).isEqualTo(CLUSTER2_ID);
    assertThat(qlCluster.getCloudProviderId()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(qlCluster.getClusterType()).isEqualTo(AWS_ECS);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterDataFetcherKubernetes() {
    when(clusterRecordService.get(CLUSTER2_ID))
        .thenReturn(
            mockDirectKubernetesClusterRecord(ACCOUNT1_ID, CLUSTER1_NAME, CLUSTER2_ID, CLOUD_PROVIDER1_ID_ACCOUNT1));
    QLCluster qlCluster =
        clusterDataFetcher.fetch(QLClusterQueryParameters.builder().clusterId(CLUSTER2_ID).build(), ACCOUNT1_ID);

    assertThat(qlCluster.getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(qlCluster.getId()).isEqualTo(CLUSTER2_ID);
    assertThat(qlCluster.getCloudProviderId()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(qlCluster.getClusterType()).isEqualTo(DIRECT_KUBERNETES);
  }

  public ClusterRecord mockEcsClusterRecord(
      String accountId, String clusterName, String clusterId, String cloudProviderId, String region) {
    Cluster cluster =
        EcsCluster.builder().clusterName(clusterName).cloudProviderId(cloudProviderId).region(region).build();
    return ClusterRecord.builder().cluster(cluster).accountId(accountId).uuid(clusterId).build();
  }

  public ClusterRecord mockDirectKubernetesClusterRecord(
      String accountId, String clusterName, String clusterId, String cloudProviderId) {
    Cluster cluster =
        DirectKubernetesCluster.builder().clusterName(clusterName).cloudProviderId(cloudProviderId).build();
    return ClusterRecord.builder().cluster(cluster).accountId(accountId).uuid(clusterId).build();
  }
}
