package software.wings.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Account.Builder.anAccount;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.generator.InfrastructureMappingGenerator;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer;
import software.wings.service.intfc.InfrastructureMappingService;

public class InfrastructureMappingServiceDBTest extends WingsBaseTest {
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private OwnerManager ownerManager;

  @Test
  public void shouldUpdateProvisioner() {
    Randomizer.Seed seed = Randomizer.seed();
    Owners owners = ownerManager.create();
    owners.add(anAccount().withUuid(generateUuid()).build());

    InfrastructureMapping infra = infrastructureMappingGenerator.ensureRandom(seed, owners);
    infra.setProvisionerId(generateUuid());
    final InfrastructureMapping updated = infrastructureMappingService.update(infra);
    assertThat(updated.getProvisionerId()).isEqualTo(infra.getProvisionerId());
  }
}
