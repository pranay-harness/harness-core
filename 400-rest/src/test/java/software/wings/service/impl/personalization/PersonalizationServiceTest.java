/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.personalization;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.GEORGE;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.peronalization.Personalization;
import software.wings.beans.peronalization.Personalization.PersonalizationKeys;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.service.intfc.personalization.PersonalizationService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.sm.StateType;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class PersonalizationServiceTest extends WingsBaseTest {
  public static final String FOO = "FOO";
  public static final String BAR = "BAR";
  public static final String BAZ = "BAZ";

  @InjectMocks @Inject PersonalizationService PersonalizationService;
  @Mock private TemplateService templateService;

  @Test
  @Owner(developers = GEORGE)
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
  @Owner(developers = GEORGE)
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
    assertThat(isEmpty(removeLast.getSteps().getFavorites())).isTrue();

    final Personalization removeFromEmpty =
        PersonalizationService.removeFavoriteStep(StateType.HTTP, accountId, userId);
    assertThat(isEmpty(removeLast.getSteps().getFavorites())).isTrue();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testNormalizeRecent() {
    LinkedList<String> recent = null;
    PersonalizationServiceImpl.normalizeRecent(recent);
    assertThat(isEmpty(recent)).isTrue();

    recent = new LinkedList<>();
    PersonalizationServiceImpl.normalizeRecent(recent);
    assertThat(isEmpty(recent)).isTrue();

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
  @Owner(developers = GEORGE)
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

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testAddFavoriteTemplate() {
    String accountId = generateUuid();
    String userId = generateUuid();

    String template1_id = generateUuid();
    String template2_id = generateUuid();
    final Template httpTemplate =
        Template.builder()
            .templateObject(HttpTemplate.builder().url("MyUrl").method("MyMethod").assertion("Assertion").build())
            .build();
    when(templateService.get(template1_id)).thenReturn(httpTemplate);
    when(templateService.get(template2_id)).thenReturn(httpTemplate);

    Set<String> expectedFavoriteTemplates = new HashSet<>();
    expectedFavoriteTemplates.add(template1_id);
    expectedFavoriteTemplates.add(template2_id);

    final Personalization addToMissingEntity =
        PersonalizationService.addFavoriteTemplate(template1_id, accountId, userId);
    assertThat(addToMissingEntity.getTemplates().getFavorites()).containsExactly(template1_id);

    final Personalization addToExisting = PersonalizationService.addFavoriteTemplate(template2_id, accountId, userId);
    assertThat(addToExisting.getTemplates().getFavorites().size()).isEqualTo(expectedFavoriteTemplates.size());
    assertThat(addToExisting.getTemplates().getFavorites()).containsAll(expectedFavoriteTemplates);

    final Personalization addSecond = PersonalizationService.addFavoriteTemplate(template2_id, accountId, userId);
    assertThat(addToExisting.getTemplates().getFavorites().size()).isEqualTo(expectedFavoriteTemplates.size());
    assertThat(addSecond.getTemplates().getFavorites()).containsAll(expectedFavoriteTemplates);

    final Personalization personalization =
        PersonalizationService.fetch(accountId, userId, asList(PersonalizationKeys.templates));
    assertThat(personalization).isNotNull();
    assertThat(personalization.getTemplates()).isNotNull();
    assertThat(personalization.getTemplates().getFavorites()).isNotNull();
    assertThat(personalization.getTemplates().getFavorites()).containsAll(expectedFavoriteTemplates);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testRemoveFavoriteTemplate() {
    String accountId = generateUuid();
    String userId = generateUuid();

    String template1_id = generateUuid();
    String template2_id = generateUuid();
    String template3_id = generateUuid();
    final Template httpTemplate =
        Template.builder()
            .templateObject(HttpTemplate.builder().url("MyUrl").method("MyMethod").assertion("Assertion").build())
            .build();
    when(templateService.get(template1_id)).thenReturn(httpTemplate);
    when(templateService.get(template2_id)).thenReturn(httpTemplate);
    when(templateService.get(template3_id)).thenReturn(httpTemplate);

    final Personalization removeFromMissingEntity =
        PersonalizationService.removeFavoriteTemplate(template1_id, accountId, userId);
    assertThat(removeFromMissingEntity.getTemplates()).isNull();

    PersonalizationService.addFavoriteTemplate(template1_id, accountId, userId);
    PersonalizationService.addFavoriteTemplate(template2_id, accountId, userId);

    Set<String> expectedFavoriteTemplates = new HashSet<>();
    expectedFavoriteTemplates.add(template1_id);
    expectedFavoriteTemplates.add(template2_id);

    final Personalization removeMissing =
        PersonalizationService.removeFavoriteTemplate(template3_id, accountId, userId);
    assertThat(removeMissing.getTemplates().getFavorites().size()).isEqualTo(expectedFavoriteTemplates.size());
    assertThat(removeMissing.getTemplates().getFavorites()).containsAll(expectedFavoriteTemplates);

    final Personalization remove = PersonalizationService.removeFavoriteTemplate(template1_id, accountId, userId);
    assertThat(remove.getTemplates().getFavorites()).containsExactly(template2_id);

    final Personalization removeLast = PersonalizationService.removeFavoriteTemplate(template2_id, accountId, userId);
    assertThat(isEmpty(removeLast.getTemplates().getFavorites())).isTrue();

    final Personalization removeFromEmpty =
        PersonalizationService.removeFavoriteTemplate(template2_id, accountId, userId);
    assertThat(isEmpty(removeLast.getTemplates().getFavorites())).isTrue();
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testFetchFavoriteTemplates() {
    String accountId = generateUuid();
    String userId = generateUuid();

    String template1_id = generateUuid();
    String template2_id = generateUuid();
    String template3_id = generateUuid();
    final Template httpTemplate =
        Template.builder()
            .templateObject(HttpTemplate.builder().url("MyUrl").method("MyMethod").assertion("Assertion").build())
            .build();
    when(templateService.get(template1_id)).thenReturn(httpTemplate);
    when(templateService.get(template2_id)).thenReturn(httpTemplate);
    when(templateService.get(template3_id)).thenReturn(httpTemplate);
    Set<String> favorites = PersonalizationService.fetchFavoriteTemplates(accountId, userId);
    assertThat(favorites).isEmpty();
    PersonalizationService.addFavoriteTemplate(template1_id, accountId, userId);
    PersonalizationService.addFavoriteTemplate(template2_id, accountId, userId);

    Set<String> expectedFavoriteTemplates = new HashSet<>();
    expectedFavoriteTemplates.add(template1_id);
    expectedFavoriteTemplates.add(template2_id);
    favorites = PersonalizationService.fetchFavoriteTemplates(accountId, userId);
    assertThat(favorites.size()).isEqualTo(2);
    assertThat(favorites).containsAll(expectedFavoriteTemplates);
  }
}
