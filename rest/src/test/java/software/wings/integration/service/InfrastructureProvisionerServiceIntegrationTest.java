package software.wings.integration.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.rule.AuthorRule.Author;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.generator.InfrastructureProvisionerGenerator;
import software.wings.generator.Randomizer;
import software.wings.integration.BaseIntegrationTest;
import software.wings.rules.Integration;
import software.wings.service.intfc.InfrastructureProvisionerService;

import java.util.List;

@Integration
public class InfrastructureProvisionerServiceIntegrationTest extends BaseIntegrationTest {
  @Inject @InjectMocks private InfrastructureProvisionerService infrastructureProvisionerService;

  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;

  @Test
  @Author(email = "george@harness.io")
  public void listForTaskTest() {
    Randomizer.Seed seed = Randomizer.seed();
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensureRandom(seed);
    final InfrastructureMappingBlueprint mappingBlueprint = infrastructureProvisioner.getMappingBlueprints().get(0);

    List<InfrastructureProvisioner> provisioners = infrastructureProvisionerService.listByBlueprintDetails(
        infrastructureProvisioner.getAppId(), infrastructureProvisioner.getInfrastructureProvisionerType(),
        mappingBlueprint.getServiceId(), mappingBlueprint.getDeploymentType(), mappingBlueprint.getCloudProviderType());

    assertThat(provisioners.size()).isEqualTo(1);

    provisioners = infrastructureProvisionerService.listByBlueprintDetails(infrastructureProvisioner.getAppId(),
        infrastructureProvisioner.getInfrastructureProvisionerType(), generateUuid(),
        mappingBlueprint.getDeploymentType(), mappingBlueprint.getCloudProviderType());

    assertThat(provisioners.size()).isEqualTo(0);
  }
}