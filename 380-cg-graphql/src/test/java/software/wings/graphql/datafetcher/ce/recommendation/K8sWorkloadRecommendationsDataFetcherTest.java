/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.AVMOHAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.ClusterRecordDao;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation;
import io.harness.ccm.commons.entities.k8s.recommendation.K8sWorkloadRecommendation.K8sWorkloadRecommendationBuilder;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadFilter;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadRecommendationPreset;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLLastDayCost;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceEntry;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceRequirement;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.security.UserThreadLocal;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import graphql.schema.DataFetchingFieldSelectionSet;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class K8sWorkloadRecommendationsDataFetcherTest extends AbstractDataFetcherTestBase {
  private static final QLPageQueryParameters DUMMY_PAGE_QUERY_PARAMS = new QLPageQueryParameters() {
    @Override
    public int getLimit() {
      return 100;
    }

    @Override
    public int getOffset() {
      return 0;
    }

    @Override
    public DataFetchingFieldSelectionSet getSelectionSet() {
      return null;
    }

    @Override
    public boolean isHasMoreRequested() {
      return false;
    }

    @Override
    public boolean isTotalRequested() {
      return false;
    }
  };

  @Inject private K8sWorkloadRecommendationsDataFetcher k8sWorkloadRecommendationsDataFetcher;
  @Inject private HPersistence hPersistence;
  @Inject private ClusterRecordDao clusterRecordDao;

  private String clusterId;

  @Before
  public void setUp() throws Exception {
    Account account = testUtils.createAccount();
    User user = testUtils.createUser(account);
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    clusterId = clusterRecordDao
                    .upsertCluster(ClusterRecord.builder()
                                       .accountId(ACCOUNT1_ID)
                                       .cluster(DirectKubernetesCluster.builder()
                                                    .clusterName(CLUSTER1_NAME)
                                                    .cloudProviderId(CLOUD_PROVIDER1_ID_ACCOUNT1)
                                                    .build())
                                       .isDeactivated(true)
                                       .build())
                    .getUuid();
  }

  // https://harness.slack.com/archives/C01JMN5P7EX/p1615213693230500?thread_ts=1615212596.227700&cid=C01JMN5P7EX
  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  @Ignore("Test is outdated. Please check the slack for details")
  public void shouldFetchRecommendation() throws Exception {
    K8sWorkloadRecommendation recommendation = getK8sWorkloadRecommendationBuilder().build();
    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = getFilters();
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes.get(0))
        .isEqualTo(QLK8sWorkloadRecommendation.builder()
                       .clusterId(clusterId)
                       .clusterName(CLUSTER1_NAME)
                       .namespace("default")
                       .workloadType("Deployment")
                       .workloadName("my-nginx")
                       .preset(QLK8sWorkloadRecommendationPreset.builder()
                                   .cpuRequest(0.8)
                                   .memoryRequest(0.8)
                                   .memoryLimit(0.95)
                                   .safetyMargin(0.15)
                                   .minCpuMilliCores(25L)
                                   .minMemoryBytes(250_000_000L)
                                   .build())
                       .containerRecommendation(QLContainerRecommendation.builder()
                                                    .containerName("nginx")
                                                    .current(QLResourceRequirement.builder()
                                                                 .request(QLResourceEntry.of("cpu", "1"))
                                                                 .request(QLResourceEntry.of("memory", "1Gi"))
                                                                 .limit(QLResourceEntry.of("cpu", "1"))
                                                                 .limit(QLResourceEntry.of("memory", "1Gi"))
                                                                 .yaml("limits:\n"
                                                                     + "  memory: 1Gi\n"
                                                                     + "  cpu: '1'\n"
                                                                     + "requests"
                                                                     + ":\n"
                                                                     + "  memory: 1Gi\n"
                                                                     + "  cpu: '1'\n")
                                                                 .build())
                                                    .burstable(QLResourceRequirement.builder()
                                                                   .request(QLResourceEntry.of("cpu", "50m"))
                                                                   .request(QLResourceEntry.of("memory", "10Mi"))
                                                                   .limit(QLResourceEntry.of("cpu", "200m"))
                                                                   .limit(QLResourceEntry.of("memory", "40Mi"))
                                                                   .yaml("limits:\n"
                                                                       + "  memory: 40Mi\n"
                                                                       + "  cpu: 200m\n"
                                                                       + "requests:\n"
                                                                       + "  memory: 10Mi\n"
                                                                       + "  cpu: 50m\n")
                                                                   .build())
                                                    .guaranteed(QLResourceRequirement.builder()
                                                                    .request(QLResourceEntry.of("cpu", "200m"))
                                                                    .request(QLResourceEntry.of("memory", "40Mi"))
                                                                    .limit(QLResourceEntry.of("cpu", "200m"))
                                                                    .limit(QLResourceEntry.of("memory", "40Mi"))
                                                                    .yaml("limits:\n"
                                                                        + "  memory: 40Mi\n"
                                                                        + "  cpu: 200m\n"
                                                                        + "requests:\n"
                                                                        + "  memory: 40Mi\n"
                                                                        + "  cpu: 200m\n")
                                                                    .build())
                                                    .recommended(QLResourceRequirement.builder()
                                                                     .request(QLResourceEntry.of("cpu", "50m"))
                                                                     .request(QLResourceEntry.of("memory", "10Mi"))
                                                                     .limit(QLResourceEntry.of("memory", "40Mi"))
                                                                     .yaml("limits:\n"
                                                                         + "  memory: 40Mi\n"
                                                                         + "requests:\n"
                                                                         + "  memory: 10Mi\n"
                                                                         + "  cpu: 50m\n")
                                                                     .build())
                                                    .p50(QLResourceRequirement.builder().build())
                                                    .p80(QLResourceRequirement.builder().build())
                                                    .p90(QLResourceRequirement.builder().build())
                                                    .p95(QLResourceRequirement.builder().build())
                                                    .p99(QLResourceRequirement.builder().build())
                                                    .numDays(7)
                                                    .build())
                       .estimatedSavings(BigDecimal.valueOf(100.0))
                       .lastDayCost(QLLastDayCost.builder().info("Not Available").build())
                       .numDays(7)
                       .build());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHideRecommendationIfNoRecentUtilData() throws Exception {
    K8sWorkloadRecommendation recommendation =
        getK8sWorkloadRecommendationBuilder().lastReceivedUtilDataAt(Instant.now().minus(Duration.ofDays(3))).build();
    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = getFilters();
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHideRecommendationIfNoLastDayCostAvailable() throws Exception {
    K8sWorkloadRecommendation recommendation =
        getK8sWorkloadRecommendationBuilder().lastDayCostAvailable(false).build();
    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = getFilters();
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHideEmptyRecommendation() throws Exception {
    K8sWorkloadRecommendation recommendation = getK8sWorkloadRecommendationBuilder()
                                                   .containerRecommendation("nginx",
                                                       ContainerRecommendation.builder()
                                                           .current(ResourceRequirement.builder()
                                                                        .request("cpu", "1")
                                                                        .request("memory", "1Gi")
                                                                        .limit("cpu", "1")
                                                                        .limit("memory", "1Gi")
                                                                        .build())
                                                           .numDays(7)
                                                           .build())
                                                   .build();

    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = getFilters();
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes).isEmpty();
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldHideRecomendationIfAnyContainerHasLessThan24hUtilData() throws Exception {
    K8sWorkloadRecommendation recommendation = getK8sWorkloadRecommendationBuilder().numDays(0).build();
    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = getFilters();
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes).isEmpty();
  }

  private K8sWorkloadRecommendationBuilder getK8sWorkloadRecommendationBuilder() {
    return K8sWorkloadRecommendation.builder()
        .accountId(ACCOUNT1_ID)
        .clusterId(clusterId)
        .namespace("default")
        .workloadType("Deployment")
        .workloadName("my-nginx")
        .lastReceivedUtilDataAt(Instant.now().minus(Duration.ofHours(12)))
        .lastDayCostAvailable(true)
        .containerRecommendation("nginx",
            ContainerRecommendation.builder()
                .current(ResourceRequirement.builder()
                             .request("cpu", "1")
                             .request("memory", "1Gi")
                             .limit("cpu", "1")
                             .limit("memory", "1Gi")
                             .build())
                .burstable(ResourceRequirement.builder()
                               .request("cpu", "50m")
                               .request("memory", "10Mi")
                               .limit("cpu", "200m")
                               .limit("memory", "40Mi")
                               .build())
                .guaranteed(ResourceRequirement.builder()
                                .request("cpu", "200m")
                                .request("memory", "40Mi")
                                .limit("cpu", "200m")
                                .limit("memory", "40Mi")
                                .build())
                .recommended(ResourceRequirement.builder()
                                 .request("cpu", "50m")
                                 .request("memory", "10Mi")
                                 .limit("memory", "40Mi")
                                 .build())
                .numDays(7)
                .build())
        .numDays(7)
        .estimatedSavings(BigDecimal.valueOf(100.0));
  }

  private ImmutableList<QLK8sWorkloadFilter> getFilters() {
    return ImmutableList.of(
        QLK8sWorkloadFilter.builder()
            .cluster(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {clusterId}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .namespace(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"default"}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .workloadName(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"my-nginx"}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .workloadType(
                QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"Deployment"}).build())
            .build());
  }
}
