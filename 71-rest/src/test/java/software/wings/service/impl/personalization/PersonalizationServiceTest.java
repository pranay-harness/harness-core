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
import software.wings.beans.peronalization.Personalization;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.sm.StateType;

import java.util.LinkedList;

public class PersonalizationServiceTest extends WingsBaseTest {
  public static final String FOO = "FOO";
  public static final String BAR = "BAR";
  public static final String BAZ = "BAZ";

  @Inject PersonalizationService PersonalizationService;

  @Test
  @Category(UnitTests.class)
  public void testAddFavoriteStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final Personalization addToMissingEntity =
        PersonalizationService.addFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addToMissingEntity.getSteps().getFavorites()).containsExactly(StateType.SHELL_SCRIPT.name());

    final Personalization addToExisting = PersonalizationService.addFavoriteStep(StateType.HTTP, accountId, userId);
    assertThat(addToExisting.getSteps().getFavorites())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final Personalization addSecond = PersonalizationService.addFavoriteStep(StateType.HTTP, accountId, userId);
    assertThat(addSecond.getSteps().getFavorites())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());
  }

  @Test
  @Category(UnitTests.class)
  public void testRemoveFavoriteStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final Personalization removeFromMissingEntity =
        PersonalizationService.removeFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(removeFromMissingEntity.getSteps()).isNull();

    PersonalizationService.addFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    PersonalizationService.addFavoriteStep(StateType.HTTP, accountId, userId);

    final Personalization removeMissing =
        PersonalizationService.removeFavoriteStep(StateType.COMMAND, accountId, userId);
    assertThat(removeMissing.getSteps().getFavorites())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final Personalization remove = PersonalizationService.removeFavoriteStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(remove.getSteps().getFavorites()).containsExactly(StateType.HTTP.name());

    final Personalization removeLast = PersonalizationService.removeFavoriteStep(StateType.HTTP, accountId, userId);
    assertTrue(isEmpty(removeLast.getSteps().getFavorites()));

    final Personalization removeFromEmpty =
        PersonalizationService.removeFavoriteStep(StateType.HTTP, accountId, userId);
    assertTrue(isEmpty(removeLast.getSteps().getFavorites()));
  }

  @Test
  @Category(UnitTests.class)
  public void testNormalizeRecent() {
    LinkedList<String> recent = null;
    PersonalizationServiceImpl.normalizeRecent(recent);
    assertTrue(isEmpty(recent));

    recent = new LinkedList<>();
    PersonalizationServiceImpl.normalizeRecent(recent);
    assertTrue(isEmpty(recent));

    recent = new LinkedList<>();
    recent.add(FOO);
    recent.add(BAZ);
    recent.add(FOO);
    recent.add(BAR);
    recent.add(FOO);
    recent.add(FOO);
    recent.add(BAR);

    PersonalizationServiceImpl.normalizeRecent(recent);
    assertThat(recent).containsExactly(BAZ, FOO, BAR);

    recent = new LinkedList<>();
    for (int i = 0; i < 20; ++i) {
      recent.add(FOO + i);
    }

    PersonalizationServiceImpl.normalizeRecent(recent);
    assertThat(recent.size()).isEqualTo(PersonalizationService.MAX_ALLOWED_RECENT);
  }

  @Test
  @Category(UnitTests.class)
  public void testAddRecentStep() {
    String accountId = generateUuid();
    String userId = generateUuid();

    final Personalization addToMissingEntity =
        PersonalizationService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addToMissingEntity.getSteps().getRecent()).containsExactly(StateType.SHELL_SCRIPT.name());

    final Personalization addToExisting = PersonalizationService.addRecentStep(StateType.HTTP, accountId, userId);
    assertThat(addToExisting.getSteps().getRecent())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name());

    final Personalization addSecond = PersonalizationService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    assertThat(addSecond.getSteps().getRecent())
        .containsExactly(StateType.SHELL_SCRIPT.name(), StateType.HTTP.name(), StateType.SHELL_SCRIPT.name());

    for (int i = 0; i < 100; ++i) {
      PersonalizationService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);
    }

    final Personalization shorten = PersonalizationService.addRecentStep(StateType.SHELL_SCRIPT, accountId, userId);

    assertThat(shorten.getSteps().getRecent().size()).isLessThan(100);
  }
}
