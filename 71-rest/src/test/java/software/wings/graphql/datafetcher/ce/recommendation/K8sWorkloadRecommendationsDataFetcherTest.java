package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.graphql.datafetcher.ce.recommendation.dto.QLWorkloadType.Deployment;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import graphql.schema.DataFetchingFieldSelectionSet;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTest;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadFilter;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceEntry;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceRequirement;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLWorkloadType;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLWorkloadTypeFilter;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.security.UserThreadLocal;

import java.util.List;

public class K8sWorkloadRecommendationsDataFetcherTest extends AbstractDataFetcherTest {
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

  @Before
  public void setUp() throws Exception {
    Account account = testUtils.createAccount();
    User user = testUtils.createUser(account);
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
  }

  @Test
  @Owner(developers = AVMOHAN)
  @Category(UnitTests.class)
  public void shouldFetchRecommendation() throws Exception {
    K8sWorkloadRecommendation recommendation =
        K8sWorkloadRecommendation.builder()
            .accountId(ACCOUNT1_ID)
            .clusterId(CLUSTER1_ID)
            .namespace("default")
            .workloadType("Deployment")
            .workloadName("my-nginx")
            .containerRecommendation(ContainerRecommendation.builder()
                                         .containerName("nginx")
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
                                         .build())
            .estimatedSavings(100.0)
            .build();
    hPersistence.save(recommendation);
    List<QLK8sWorkloadFilter> filters = ImmutableList.of(
        QLK8sWorkloadFilter.builder()
            .cluster(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {CLUSTER1_ID}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .namespace(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"default"}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .workloadName(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {"my-nginx"}).build())
            .build(),
        QLK8sWorkloadFilter.builder()
            .workloadType(QLWorkloadTypeFilter.builder()
                              .operator(QLEnumOperator.EQUALS)
                              .values(new QLWorkloadType[] {Deployment})
                              .build())
            .build());
    QLK8SWorkloadRecommendationConnection qlk8SWorkloadRecommendationConnection =
        k8sWorkloadRecommendationsDataFetcher.fetchConnection(filters, DUMMY_PAGE_QUERY_PARAMS, null);
    List<QLK8sWorkloadRecommendation> nodes = qlk8SWorkloadRecommendationConnection.getNodes();
    assertThat(nodes.get(0))
        .isEqualTo(QLK8sWorkloadRecommendation.builder()
                       .namespace("default")
                       .workloadType("Deployment")
                       .workloadName("my-nginx")
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
                                                    .build())
                       .estimatedSavings(100.0)
                       .build());
  }
}
