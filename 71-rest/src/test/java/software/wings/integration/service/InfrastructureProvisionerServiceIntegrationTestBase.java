package software.wings.integration.service;

import com.google.inject.Inject;

import org.mockito.InjectMocks;
import software.wings.integration.IntegrationTestBase;
import software.wings.rules.Integration;
import software.wings.service.intfc.InfrastructureProvisionerService;

@Integration
public class InfrastructureProvisionerServiceIntegrationTestBase extends IntegrationTestBase {
  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;

  //  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  //
  //  @Test
  //  @Owner(emails = UNKNOWN)
  //  @Owner(emails = GEORGE)
  //  public void listForTaskTest() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    final InfrastructureProvisioner infrastructureProvisioner =
  //    infrastructureProvisionerGenerator.ensureRandom(seed); final InfrastructureMappingBlueprint mappingBlueprint =
  //    infrastructureProvisioner.getMappingBlueprints().get(0);
  //
  //    List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.listByBlueprintDetails(
  //        infrastructureProvisioner.getApplicationId(), infrastructureProvisioner.getInfrastructureProvisionerType(),
  //        mappingBlueprint.getManifestByServiceId(), mappingBlueprint.getDeploymentType(),
  //        mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(1);
  //
  //    provisioners =
  //    infrastructureProvisionerService.listByBlueprintDetails(infrastructureProvisioner.getApplicationId(),
  //        infrastructureProvisioner.getInfrastructureProvisionerType(), generateUuid(),
  //        mappingBlueprint.getDeploymentType(), mappingBlueprint.getCloudProviderType());
  //
  //    assertThat(provisioners.size()).isEqualTo(0);
  //  }
}
