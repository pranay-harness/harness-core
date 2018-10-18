package software.wings.service;

import com.google.inject.Inject;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.InfrastructureMappingService;

public class InfrastructureMappingServiceDBTest extends WingsBaseTest {
  @Inject private InfrastructureMappingService infrastructureMappingService;

  //  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;
  //  @Inject private OwnerManager ownerManager;
  //
  //  @Test
  //  public void shouldUpdateProvisioner() {
  //    Randomizer.Seed seed = Randomizer.seed();
  //    Owners owners = ownerManager.create();
  //    owners.add(anAccount().withUuid(generateUuid()).build());
  //
  //    InfrastructureMapping infra = infrastructureMappingGenerator.ensureRandom(seed, owners);
  //    infra.setProvisionerId(generateUuid());
  //    final InfrastructureMapping updated = infrastructureMappingService.update(infra);
  //    assertThat(updated.getProvisionerId()).isEqualTo(infra.getProvisionerId());
  //  }
}
