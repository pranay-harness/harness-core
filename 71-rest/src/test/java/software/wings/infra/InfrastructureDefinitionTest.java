package software.wings.infra;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.infra.InfraDefinitionTestConstants.RELEASE_NAME;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.utils.WingsTestConstants;

public class InfrastructureDefinitionTest extends CategoryTest {
  private InfraMappingInfrastructureProvider mappingProvider;

  private InfrastructureDefinition infrastructureDefinition;

  @After
  public void tearDown() throws Exception {
    mappingProvider = null;
    infrastructureDefinition = null;
  }

  @Before
  public void setUp() throws Exception {
    mappingProvider = GoogleKubernetesEngine.builder()
                          .clusterName(WingsTestConstants.CLUSTER_NAME)
                          .cloudProviderId(WingsTestConstants.COMPUTE_PROVIDER_ID)
                          .namespace(WingsTestConstants.NAMESPACE)
                          .releaseName(RELEASE_NAME)
                          .build();

    infrastructureDefinition = InfrastructureDefinition.builder()
                                   .infrastructure(mappingProvider)
                                   .appId(WingsTestConstants.APP_ID)
                                   .cloudProviderType(CloudProviderType.GCP)
                                   .deploymentType(DeploymentType.KUBERNETES)
                                   .envId(WingsTestConstants.ENV_ID)
                                   .name("GCPInfra")
                                   .build();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    InfrastructureMapping infrastructureMapping = infrastructureDefinition.getInfraMapping();
    assertThat(infrastructureDefinition.getInfrastructure().getMappingClass())
        .isEqualTo(infrastructureMapping.getClass());

    GcpKubernetesInfrastructureMapping infraMapping = (GcpKubernetesInfrastructureMapping) infrastructureMapping;

    assertThat(RELEASE_NAME).isEqualTo(infraMapping.getReleaseName());
    assertThat(WingsTestConstants.NAMESPACE).isEqualTo(infraMapping.getNamespace());
    assertThat(WingsTestConstants.COMPUTE_PROVIDER_ID).isEqualTo(infraMapping.getComputeProviderSettingId());
    assertThat(WingsTestConstants.CLUSTER_NAME).isEqualTo(infraMapping.getClusterName());
  }
}