package software.wings.service.impl.personalization;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.peronalization.PersonalizationStep;
import software.wings.service.intfc.personalization.PersonalizationStepService;
import software.wings.sm.StateType;

import java.util.LinkedList;

public class PersonalizationStepServiceTest extends WingsBaseTest {
  public static final String FOO = "FOO";
  public static final String BAR = "BAR";
  public static final String BAZ = "BAZ";

  @Inject PersonalizationStepService personalizationStepService;

  @Test
  @Category(UnitTests.class)
  public void testAddFavoriteStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final PersonalizationStep addToMissingEntity =
        personalizationStepService.addFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addToMissingEntity.getFavorites()).containsExactly(StateType.SHELL_SCRIPT.name());

    final PersonalizationStep addToExisting =
        personalizationStepService.addFavoriteStep(StateType.HTTP, accountId, userId);
    assertThat(addToExisting.getFavorites()).containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final PersonalizationStep addSecond = personalizationStepService.addFavoriteStep(StateType.HTTP, accountId, userId);
    assertThat(addSecond.getFavorites()).containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());
  }

  @Test
  @Category(UnitTests.class)
  public void testRemoveFavoriteStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final PersonalizationStep removeFromMissingEntity =
        personalizationStepService.removeFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertTrue(isEmpty(removeFromMissingEntity.getFavorites()));

    personalizationStepService.addFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    personalizationStepService.addFavoriteStep(StateType.HTTP, accountId, userId);

    final PersonalizationStep removeMissing =
        personalizationStepService.removeFavoriteStep(StateType.COMMAND, accountId, userId);
    assertThat(removeMissing.getFavorites()).containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final PersonalizationStep remove =
        personalizationStepService.removeFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(remove.getFavorites()).containsExactly(StateType.HTTP.name());

    final PersonalizationStep removeLast =
        personalizationStepService.removeFavoriteStep(StateType.HTTP, accountId, userId);
    assertTrue(isEmpty(removeLast.getFavorites()));

    final PersonalizationStep removeFromEmpty =
        personalizationStepService.removeFavoriteStep(StateType.HTTP, accountId, userId);
    assertTrue(isEmpty(removeLast.getFavorites()));
  }

  @Test
  @Category(UnitTests.class)
  public void testNormalizeRecent() {
    LinkedList<String> recent = null;
    PersonalizationStepServiceImpl.normalizeRecent(recent);
    assertTrue(isEmpty(recent));

    recent = new LinkedList<>();
    PersonalizationStepServiceImpl.normalizeRecent(recent);
    assertTrue(isEmpty(recent));

    recent = new LinkedList<>();
    recent.add(FOO);
    recent.add(BAZ);
    recent.add(FOO);
    recent.add(BAR);
    recent.add(FOO);
    recent.add(FOO);
    recent.add(BAR);

    PersonalizationStepServiceImpl.normalizeRecent(recent);
    assertThat(recent).containsExactly(BAZ, FOO, BAR);

    recent = new LinkedList<>();
    for (int i = 0; i < 20; ++i) {
      recent.add(FOO + i);
    }

    PersonalizationStepServiceImpl.normalizeRecent(recent);
    assertThat(recent.size()).isEqualTo(PersonalizationStepService.MAX_ALLOWED_RECENT);
  }

  @Test
  @Category(UnitTests.class)
  public void testAddRecentStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final PersonalizationStep addToMissingEntity =
        personalizationStepService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addToMissingEntity.getRecent()).containsExactly(StateType.SHELL_SCRIPT.name());

    final PersonalizationStep addToExisting =
        personalizationStepService.addRecentStep(StateType.HTTP, accountId, userId);
    assertThat(addToExisting.getRecent()).containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final PersonalizationStep addSecond =
        personalizationStepService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addSecond.getRecent())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name(), StateType.SHELL_SCRIPT.name());

    for (int i = 0; i < 100; ++i) {
      personalizationStepService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    }

    final PersonalizationStep shorten =
        personalizationStepService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);

    assertThat(shorten.getRecent().size()).isLessThan(100);
  }
}
