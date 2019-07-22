package software.wings.infra;

import static org.junit.Assert.assertEquals;
import static software.wings.infra.InfraDefinitionTestConstants.RELEASE_NAME;

import io.harness.category.element.UnitTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.utils.WingsTestConstants;

public class InfrastructureDefinitionTest {
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
  @Category(UnitTests.class)
  public void testGetInfraMapping() {
    InfrastructureMapping infrastructureMapping = infrastructureDefinition.getInfraMapping();
    assertEquals(infrastructureMapping.getClass(), infrastructureDefinition.getInfrastructure().getMappingClass());

    GcpKubernetesInfrastructureMapping infraMapping = (GcpKubernetesInfrastructureMapping) infrastructureMapping;

    assertEquals(infraMapping.getReleaseName(), RELEASE_NAME);
    assertEquals(infraMapping.getNamespace(), WingsTestConstants.NAMESPACE);
    assertEquals(infraMapping.getComputeProviderSettingId(), WingsTestConstants.COMPUTE_PROVIDER_ID);
    assertEquals(infraMapping.getClusterName(), WingsTestConstants.CLUSTER_NAME);
  }
}