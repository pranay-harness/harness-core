package software.wings.integration.service;

import com.google.inject.Inject;

import org.mockito.InjectMocks;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;
import software.wings.service.intfc.InfrastructureProvisionerService;

@Integration
public class InfrastructureProvisionerServiceIntegrationTest extends BaseIntegrationTest {
  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;

  //  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  //
  //  @Test
  //  @Owner(emails = "george@harness.io", resent = false)
  //  public void listForTaskTest() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final InfrastructureMappingBlueprint mappingBlueprint =
  //    infrastructureProvisioner.getMappingBlueprints().get(0);
  //
  //    List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.listByBlueprintDetails(
  //        infrastructureProvisioner.getAppId(), infrastructureProvisioner.getInfrastructureProvisionerType(),
  //        mappingBlueprint.getServiceId(), mappingBlueprint.getDeploymentType(),
  //        mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(1);
  //
  //    provisioners = infrastructureProvisionerService.listByBlueprintDetails(infrastructureProvisioner.getAppId(),
  //        infrastructureProvisioner.getInfrastructureProvisionerType(), generateUuid(),
  //        mappingBlueprint.getDeploymentType(), mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(0);
  //  }
}