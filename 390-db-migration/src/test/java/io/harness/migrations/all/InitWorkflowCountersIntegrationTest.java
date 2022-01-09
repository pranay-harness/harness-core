/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.rule.OwnerRule.UJJAWAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.counter.service.CounterService;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.beans.Workflow;
import software.wings.integration.IntegrationTestBase;
import software.wings.service.intfc.AppService;
import software.wings.utils.WingsIntegrationTestConstants;

import com.google.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class InitWorkflowCountersIntegrationTest extends IntegrationTestBase {
  @Inject private InitWorkflowCounters initWorkflowCounters;
  @Inject private AppService appService;
  @Inject private CounterService counterService;

  @Before
  public void init() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_WORKFLOW.toString()));
  }

  @Test
  @Owner(developers = UJJAWAL, intermittent = true)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("skipping the integration test")
  public void testMigrate() {
    long totalWorkflows = wingsPersistence.createQuery(Workflow.class).count();
    if (totalWorkflows == 0) {
      return;
    }

    Action action = new Action(WingsIntegrationTestConstants.INTEGRATION_TEST_ACCOUNT_ID, ActionType.CREATE_WORKFLOW);
    Counter initialCount = counterService.get(action);
    assertThat(initialCount).isNull();

    initWorkflowCounters.migrate();

    Set<String> appIds = appService.getAppsByAccountId(WingsIntegrationTestConstants.INTEGRATION_TEST_ACCOUNT_ID)
                             .stream()
                             .map(Application::getUuid)
                             .collect(Collectors.toSet());

    long workflowCount = wingsPersistence.createQuery(Workflow.class).field("appId").in(appIds).count();

    Counter counter = counterService.get(action);

    assertThat(0).isNotEqualTo((long) counter.getValue());
    assertThat((long) counter.getValue()).isEqualTo(workflowCount);
  }
}
