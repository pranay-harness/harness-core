package software.wings.service.impl;

import static io.harness.rule.OwnerRule.RIHAZ;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.PROVISIONER_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.AzureKubernetesInfrastructureMapping;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMappingWinRm;
import software.wings.beans.SyncTaskContext;
import software.wings.helpers.ext.container.ContainerMasterUrlHelper;

import java.util.List;

public class InfrastructureMappingServiceImplTest extends WingsBaseTest {
  @Inject InfrastructureMappingServiceImpl infrastructureMappingService;
  InfrastructureMappingServiceImpl spyInfrastructureMappingService = spy(new InfrastructureMappingServiceImpl());
  @Mock ContainerMasterUrlHelper containerMasterUrlHelper;

  private static final String DEFAULT = "default";
  private static final String USER_INPUT_NAMESPACE = "USER_INPUT_NAMESPACE";
  private static final String DEFAULT_MASTER_URL = "DEFAULT_MASTER_URL";
  private static final String MASTER_URL = "MASTER_URL";
  private static List<String> hosts = ImmutableList.of("ABC ", "", HOST_NAME);

  @Before
  public void setUp() throws Exception {
    Reflect.on(spyInfrastructureMappingService).set("containerMasterUrlHelper", containerMasterUrlHelper);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void hostsListShouldReturnEmptyWhenDynamicInfra() {
    InfrastructureMapping infraMapping = PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                                             .withProvisionerId(PROVISIONER_ID)
                                             .build();

    List<String> hostDisplayNames = infrastructureMappingService.getInfrastructureMappingHostDisplayNames(
        infraMapping, APP_ID, WORKFLOW_EXECUTION_ID);

    assertThat(hostDisplayNames).isEmpty();
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsTestPhysicalInfraWinRmTest() {
    PhysicalInfrastructureMappingWinRm physicalInfraMappingWinRm =
        PhysicalInfrastructureMappingWinRm.Builder.aPhysicalInfrastructureMappingWinRm()
            .withHostNames(hosts)
            .withInfraMappingType(InfrastructureType.PHYSICAL_INFRA_WINRM)
            .build();

    infrastructureMappingService.setDefaults(physicalInfraMappingWinRm);

    assertThat(physicalInfraMappingWinRm.getHostNames().size()).isEqualTo(2);
    assertThat(physicalInfraMappingWinRm.getHostNames().get(0)).isEqualTo("ABC");
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsPhysicalInfraTest() {
    PhysicalInfrastructureMapping physicalInfraMapping =
        PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
            .withHostNames(hosts)
            .withInfraMappingType(InfrastructureType.PHYSICAL_INFRA)
            .build();

    infrastructureMappingService.setDefaults(physicalInfraMapping);

    assertThat(physicalInfraMapping.getHostNames().size()).isEqualTo(2);
    assertThat(physicalInfraMapping.getHostNames().get(0)).isEqualTo("ABC");
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsGcpInfraTest() {
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    GcpKubernetesInfrastructureMapping gcpKubernetesInfraMapping =
        GcpKubernetesInfrastructureMapping.builder().infraMappingType(InfrastructureType.GCP_KUBERNETES_ENGINE).build();

    SyncTaskContext syncTaskContext = SyncTaskContext.builder().build();

    Mockito.when(containerMasterUrlHelper.masterUrlRequiredWithProvisioner(gcpKubernetesInfraMapping)).thenReturn(true);
    doReturn(containerServiceParams)
        .when(spyInfrastructureMappingService)
        .getGcpContainerServiceParams(gcpKubernetesInfraMapping);

    doReturn(syncTaskContext).when(spyInfrastructureMappingService).getSyncTaskContext(gcpKubernetesInfraMapping);

    Mockito.when(containerMasterUrlHelper.fetchMasterUrl(containerServiceParams, syncTaskContext))
        .thenReturn(MASTER_URL);

    spyInfrastructureMappingService.setDefaults(gcpKubernetesInfraMapping);

    assertThat(gcpKubernetesInfraMapping.getMasterUrl()).isEqualTo(MASTER_URL);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsGcpInfraNoChangeTest() {
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    SyncTaskContext syncTaskContext = SyncTaskContext.builder().build();
    GcpKubernetesInfrastructureMapping gcpKubernetesInfraMapping =
        GcpKubernetesInfrastructureMapping.builder()
            .infraMappingType(InfrastructureType.GCP_KUBERNETES_ENGINE)
            .masterUrl(DEFAULT_MASTER_URL)
            .build();

    doReturn(containerServiceParams)
        .when(spyInfrastructureMappingService)
        .getGcpContainerServiceParams(gcpKubernetesInfraMapping);

    doReturn(syncTaskContext).when(spyInfrastructureMappingService).getSyncTaskContext(gcpKubernetesInfraMapping);

    Mockito.when(containerMasterUrlHelper.fetchMasterUrl(containerServiceParams, syncTaskContext))
        .thenReturn(MASTER_URL);

    spyInfrastructureMappingService.setDefaults(gcpKubernetesInfraMapping);

    assertThat(gcpKubernetesInfraMapping.getMasterUrl()).isEqualTo(DEFAULT_MASTER_URL);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsAzureKubernetesInfraTest() {
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    AzureKubernetesInfrastructureMapping azureKubernetesInfraMapping =
        AzureKubernetesInfrastructureMapping.Builder.anAzureKubernetesInfrastructureMapping()
            .withInfraMappingType(InfrastructureType.AZURE_KUBERNETES)
            .build();
    doReturn(containerServiceParams)
        .when(spyInfrastructureMappingService)
        .getAzureContainerServiceParams(azureKubernetesInfraMapping);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder().build();
    doReturn(syncTaskContext).when(spyInfrastructureMappingService).getSyncTaskContext(azureKubernetesInfraMapping);
    Mockito.when(containerMasterUrlHelper.fetchMasterUrl(containerServiceParams, syncTaskContext))
        .thenReturn(MASTER_URL);

    Mockito.when(containerMasterUrlHelper.masterUrlRequiredWithProvisioner(azureKubernetesInfraMapping))
        .thenReturn(true);

    spyInfrastructureMappingService.setDefaults(azureKubernetesInfraMapping);

    assertThat(azureKubernetesInfraMapping.getMasterUrl()).isEqualTo(MASTER_URL);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsAzureKubernetesInfraNoChangeTest() {
    ContainerServiceParams containerServiceParams = ContainerServiceParams.builder().build();
    AzureKubernetesInfrastructureMapping azureKubernetesInfraMapping =
        AzureKubernetesInfrastructureMapping.Builder.anAzureKubernetesInfrastructureMapping()
            .withInfraMappingType(InfrastructureType.AZURE_KUBERNETES)
            .withMasterUrl(DEFAULT_MASTER_URL)
            .build();
    doReturn(containerServiceParams)
        .when(spyInfrastructureMappingService)
        .getAzureContainerServiceParams(azureKubernetesInfraMapping);
    SyncTaskContext syncTaskContext = SyncTaskContext.builder().build();
    doReturn(syncTaskContext).when(spyInfrastructureMappingService).getSyncTaskContext(azureKubernetesInfraMapping);
    Mockito.when(containerMasterUrlHelper.fetchMasterUrl(containerServiceParams, syncTaskContext))
        .thenReturn(MASTER_URL);

    spyInfrastructureMappingService.setDefaults(azureKubernetesInfraMapping);

    assertThat(azureKubernetesInfraMapping.getMasterUrl()).isEqualTo(DEFAULT_MASTER_URL);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsDirectKubernetesTest() {
    DirectKubernetesInfrastructureMapping kubernetesInfrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder().infraMappingType(InfrastructureType.DIRECT_KUBERNETES).build();

    spyInfrastructureMappingService.setDefaults(kubernetesInfrastructureMapping);

    assertThat(kubernetesInfrastructureMapping.getNamespace()).isEqualTo(DEFAULT);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsDirectKubernetesNoChangeTest() {
    DirectKubernetesInfrastructureMapping kubernetesInfrastructureMapping =
        DirectKubernetesInfrastructureMapping.builder()
            .infraMappingType(InfrastructureType.DIRECT_KUBERNETES)
            .namespace(USER_INPUT_NAMESPACE)
            .build();

    spyInfrastructureMappingService.setDefaults(kubernetesInfrastructureMapping);

    assertThat(kubernetesInfrastructureMapping.getNamespace()).isEqualTo(USER_INPUT_NAMESPACE);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultGoogleKubernetesEnginesTest() {
    GcpKubernetesInfrastructureMapping gcpKubernetesInfraMapping =
        GcpKubernetesInfrastructureMapping.builder().infraMappingType(InfrastructureType.GCP_KUBERNETES_ENGINE).build();

    spyInfrastructureMappingService.setDefaults(gcpKubernetesInfraMapping);

    assertThat(gcpKubernetesInfraMapping.getNamespace()).isEqualTo(DEFAULT);
  }

  @Test
  @Owner(developers = RIHAZ)
  @Category(UnitTests.class)
  public void setDefaultsNGoogleKubernetesEngineoChangeTest() {
    GcpKubernetesInfrastructureMapping gcpKubernetesInfraMapping =
        GcpKubernetesInfrastructureMapping.builder()
            .infraMappingType(InfrastructureType.GCP_KUBERNETES_ENGINE)
            .namespace(USER_INPUT_NAMESPACE)
            .build();

    spyInfrastructureMappingService.setDefaults(gcpKubernetesInfraMapping);

    assertThat(gcpKubernetesInfraMapping.getNamespace()).isEqualTo(USER_INPUT_NAMESPACE);
  }
}