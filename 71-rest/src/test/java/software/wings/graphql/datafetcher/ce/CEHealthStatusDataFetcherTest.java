package software.wings.graphql.datafetcher.ce;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import graphql.schema.DataFetchingEnvironment;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.health.CEError;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.graphql.schema.type.aggregation.cloudprovider.CEHealthStatusDTO;
import software.wings.graphql.schema.type.aggregation.cloudprovider.ClusterErrorsDTO;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CEHealthStatusDataFetcherTest extends CategoryTest {
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String clusterId = "CLUSTER_ID";
  private CEError error = CEError.PERPETUAL_TASK_MISSING_HEARTBEAT;
  private Map<String, List<CEError>> clusterErrorMap = new HashMap<>();
  private CEHealthStatus ceHealthStatus =
      CEHealthStatus.builder().isHealthy(false).clusterErrors(clusterErrorMap).build();
  @Mock private DataFetchingEnvironment environment;
  private QLKubernetesClusterCloudProvider cloudProvider =
      QLKubernetesClusterCloudProvider.builder().id(cloudProviderId).build();
  private ClusterErrorsDTO clusterErrorsDTO;
  private CEHealthStatusDTO ceHealthStatusDTO;

  @Mock private HealthStatusService healthStatusService;
  @InjectMocks CEHealthStatusDataFetcher ceHealthStatusDataFetcher;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    clusterErrorMap.put(clusterId, Arrays.asList(error));
    clusterErrorsDTO = ClusterErrorsDTO.builder().clusterErrors(Arrays.asList(error)).build();
    ceHealthStatusDTO =
        CEHealthStatusDTO.builder().isHealthy(false).clusterErrors(Arrays.asList(clusterErrorsDTO)).build();
    when(environment.getSource()).thenReturn(cloudProvider);
    when(healthStatusService.getHealthStatus(eq(cloudProviderId))).thenReturn(ceHealthStatus);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetCEHealthStatusDTO() throws Exception {
    CEHealthStatusDTO ceHealthStatusDTO = ceHealthStatusDataFetcher.get(environment);
    assertThat(ceHealthStatusDTO).isEqualTo(ceHealthStatusDTO);
  }
}
