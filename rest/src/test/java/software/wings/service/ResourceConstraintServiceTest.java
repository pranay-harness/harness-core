package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.ResourceConstraint;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.Randomizer;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.ResourceConstraintGenerator;
import software.wings.service.intfc.ResourceConstraintService;

public class ResourceConstraintServiceTest extends WingsBaseTest {
  @Inject private OwnerManager ownersManager;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;

  @Inject private ResourceConstraintService resourceConstraintService;

  private Seed seed;
  private Owners owners;

  @Before
  public void setUp() {
    seed = Randomizer.seed();
    owners = ownersManager.create();
  }

  @Test
  public void shouldSave() {
    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
    resourceConstraint.setUuid(null);

    final ResourceConstraint newResourceConstraint = resourceConstraintService.save(resourceConstraint);
  }

  @Test
  public void shouldUpdate() {
    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
    resourceConstraint.setCapacity(resourceConstraint.getCapacity() + 5);

    final ResourceConstraint updatedResourceConstraint = resourceConstraintService.update(resourceConstraint);
    final ResourceConstraint obtainedResourceConstraint =
        resourceConstraintService.get(resourceConstraint.getAccountId(), resourceConstraint.getUuid());

    assertThat(updatedResourceConstraint.getCapacity()).isEqualTo(obtainedResourceConstraint.getCapacity());
  }

  @Test
  public void shouldGet() {
    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
    final ResourceConstraint obtainedResourceConstraint =
        resourceConstraintService.get(resourceConstraint.getAccountId(), resourceConstraint.getUuid());

    assertThat(resourceConstraint.getCapacity()).isEqualTo(obtainedResourceConstraint.getCapacity());
  }

  @Test
  public void shouldDelete() {
    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureRandom(seed, owners);
    resourceConstraintService.delete(resourceConstraint.getAccountId(), resourceConstraint.getUuid());
    assertThat(resourceConstraintService.get(resourceConstraint.getAccountId(), resourceConstraint.getUuid())).isNull();
  }
}
