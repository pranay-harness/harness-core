package software.wings.service;

import software.wings.WingsBaseTest;

public class ResourceConstraintServiceTest extends WingsBaseTest {
  //  @Inject private OwnerManager ownersManager;
  //  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  //
  //  @Inject private ResourceConstraintService resourceConstraintService;
  //
  //  private Seed seed;
  //  private Owners owners;
  //
  //  @Before
  //  public void setUp() {
  //    seed = Randomizer.seed();
  //    owners = ownersManager.create();
  //  }
  //
  //  @Test
  //  public void shouldSave() {
  //    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
  //    resourceConstraint.setUuid(null);
  //
  //    final ResourceConstraint newResourceConstraint = resourceConstraintService.save(resourceConstraint);
  //  }
  //
  //  @Test
  //  public void shouldUpdate() {
  //    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
  //    resourceConstraint.setCapacity(resourceConstraint.getCapacity() + 5);
  //
  //    final ResourceConstraint updatedResourceConstraint = resourceConstraintService.update(resourceConstraint);
  //    final ResourceConstraint obtainedResourceConstraint =
  //        resourceConstraintService.get(resourceConstraint.getAccountId(), resourceConstraint.getUuid());
  //
  //    assertThat(updatedResourceConstraint.getCapacity()).isEqualTo(obtainedResourceConstraint.getCapacity());
  //  }
  //
  //  @Test
  //  public void shouldGet() {
  //    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
  //    final ResourceConstraint obtainedResourceConstraint =
  //        resourceConstraintService.get(resourceConstraint.getAccountId(), resourceConstraint.getUuid());
  //
  //    assertThat(resourceConstraint.getCapacity()).isEqualTo(obtainedResourceConstraint.getCapacity());
  //  }
  //
  //  @Test
  //  public void shouldDelete() {
  //    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
  //    resourceConstraintService.delete(resourceConstraint.getAccountId(), resourceConstraint.getUuid());
  //    assertThat(resourceConstraintService.get(resourceConstraint.getAccountId(),
  //    resourceConstraint.getUuid())).isNull();
  //  }
}
