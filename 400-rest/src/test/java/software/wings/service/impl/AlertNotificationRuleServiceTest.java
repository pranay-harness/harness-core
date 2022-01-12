/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ANKIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.alerts.AlertCategory;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.service.intfc.AlertNotificationRuleService;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class AlertNotificationRuleServiceTest extends WingsBaseTest {
  private static final String ACCOUNT_1_ID = "DummyAccountId1";
  private static final String ACCOUNT_2_ID = "DummyAccountId2";
  private static final String USER_GROUP = "DummyUserGroup";

  @Inject @InjectMocks private AlertNotificationRuleService alertNotificationRuleService;

  @Inject private HPersistence persistence;

  @Before
  public void purgeAllRules() {
    persistence.delete(persistence.createQuery(AlertNotificationRule.class));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldCreateAlertNotificationRules() {
    List<AlertNotificationRule> rules =
        Arrays.asList(new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.Setup, null, Collections.emptySet()),
            new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.Approval, null, Collections.emptySet()),
            new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.ManualIntervention, null, Collections.emptySet()),
            new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.All, null, Collections.emptySet()));

    rules.forEach(alertNotificationRuleService::create);

    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID)).isEqualTo(rules);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotCreateMoreThanOneDefaultAlertNotificationRulesInSameAccount() {
    List<AlertNotificationRule> rules = Arrays.asList(
        createDefaultAlertNotificationRule(ACCOUNT_1_ID), createDefaultAlertNotificationRule(ACCOUNT_1_ID));

    rules.forEach(alertNotificationRuleService::create);
  }

  private AlertNotificationRule createDefaultAlertNotificationRule(String accountId) {
    return new AlertNotificationRule(accountId, AlertCategory.All, null, new HashSet<>());
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldDeleteAllAlertNotificationRulesOfAnAccount() {
    List<AlertNotificationRule> rulesOfAccount1 = Arrays.asList(createDefaultAlertNotificationRule(ACCOUNT_1_ID),
        new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.Setup, null, Collections.emptySet()));
    List<AlertNotificationRule> rulesOfAccount2 = Arrays.asList(createDefaultAlertNotificationRule(ACCOUNT_2_ID),
        new AlertNotificationRule(ACCOUNT_2_ID, AlertCategory.ManualIntervention, null, Collections.emptySet()));

    rulesOfAccount1.forEach(alertNotificationRuleService::create);
    rulesOfAccount2.forEach(alertNotificationRuleService::create);

    alertNotificationRuleService.deleteByAccountId(ACCOUNT_1_ID);

    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID)).isEmpty();
    assertThat(alertNotificationRuleService.getAll(ACCOUNT_2_ID)).isEqualTo(rulesOfAccount2);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldDeleteNonDefaultAlertNotificationRuleGivenRuleIdAndAccountId() {
    AlertNotificationRule ruleInAccount1 =
        new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.Setup, null, Collections.emptySet());
    AlertNotificationRule ruleInAccount2 =
        new AlertNotificationRule(ACCOUNT_2_ID, AlertCategory.Setup, null, Collections.emptySet());

    AlertNotificationRule savedRuleInAccount1 = alertNotificationRuleService.create(ruleInAccount1);
    AlertNotificationRule savedRuleInAccount2 = alertNotificationRuleService.create(ruleInAccount2);

    alertNotificationRuleService.deleteById(savedRuleInAccount1.getUuid(), savedRuleInAccount1.getAccountId());

    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID)).isEmpty();
    assertThat(alertNotificationRuleService.getAll(ACCOUNT_2_ID)).hasSize(1);
    assertThat(alertNotificationRuleService.getAll(ACCOUNT_2_ID).get(0)).isEqualTo(savedRuleInAccount2);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotDeleteDefaultAlertNotificationRuleOfAnAccount() {
    alertNotificationRuleService.create(createDefaultAlertNotificationRule(ACCOUNT_1_ID));
    AlertNotificationRule savedDefaultRule = alertNotificationRuleService.getAll(ACCOUNT_1_ID).get(0);

    alertNotificationRuleService.deleteById(savedDefaultRule.getUuid(), savedDefaultRule.getAccountId());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldNotUpdateNonExistingAlertNotificationRule() {
    alertNotificationRuleService.update(createDefaultAlertNotificationRule(ACCOUNT_1_ID));
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldUpdateExistingDefaultAlertNotificationRule() {
    AlertNotificationRule savedRule =
        alertNotificationRuleService.create(createDefaultAlertNotificationRule(ACCOUNT_1_ID));

    AlertNotificationRule updatedAlertNotificationRule = new AlertNotificationRule(savedRule.getAccountId(),
        savedRule.getAlertCategory(), savedRule.getAlertFilter(), new HashSet<>(Collections.singletonList(USER_GROUP)));
    updatedAlertNotificationRule.setUuid(savedRule.getUuid());

    alertNotificationRuleService.update(updatedAlertNotificationRule);

    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID)).hasSize(1);
    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID).get(0)).isEqualTo(updatedAlertNotificationRule);
  }

  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void shouldUpdateExistingNonDefaultAlertNotificationRule() {
    AlertNotificationRule savedRule = alertNotificationRuleService.create(
        new AlertNotificationRule(ACCOUNT_1_ID, AlertCategory.Setup, null, Collections.emptySet()));

    AlertNotificationRule updatedAlertNotificationRule =
        new AlertNotificationRule(savedRule.getAccountId(), AlertCategory.ManualIntervention,
            savedRule.getAlertFilter(), new HashSet<>(Collections.singletonList(USER_GROUP)));
    updatedAlertNotificationRule.setUuid(savedRule.getUuid());

    alertNotificationRuleService.update(updatedAlertNotificationRule);

    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID)).hasSize(1);
    assertThat(alertNotificationRuleService.getAll(ACCOUNT_1_ID).get(0)).isEqualTo(updatedAlertNotificationRule);
  }
}
