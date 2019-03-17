package migrations.all;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.Counter;
import io.harness.limits.counter.service.CounterService;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Workflow;
import software.wings.integration.BaseIntegrationTest;
import software.wings.service.intfc.AppService;
import software.wings.utils.WingsIntegrationTestConstants;

import java.util.Set;
import java.util.stream.Collectors;

public class InitWorkflowCountersIntegrationTest extends BaseIntegrationTest {
  @Inject private InitWorkflowCounters initWorkflowCounters;
  @Inject private AppService appService;
  @Inject private CounterService counterService;

  @Before
  public void init() {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Counter.class).field("key").endsWith(ActionType.CREATE_WORKFLOW.toString()));
  }

  @Test
  @Owner(emails = "jatin@harness.io", intermittent = true)
  @Category(IntegrationTests.class)
  public void testMigrate() {
    long totalWorkflows = wingsPersistence.createQuery(Workflow.class).count();
    if (totalWorkflows == 0) {
      return;
    }

    Action action = new Action(WingsIntegrationTestConstants.INTEGRATION_TEST_ACCOUNT_ID, ActionType.CREATE_WORKFLOW);
    Counter initialCount = counterService.get(action);
    assertNull(initialCount);

    initWorkflowCounters.migrate();

    Set<String> appIds = appService.getAppsByAccountId(WingsIntegrationTestConstants.INTEGRATION_TEST_ACCOUNT_ID)
                             .stream()
                             .map(Application::getUuid)
                             .collect(Collectors.toSet());

    long workflowCount = wingsPersistence.createQuery(Workflow.class).field("appId").in(appIds).count();

    Counter counter = counterService.get(action);

    assertNotEquals(
        "[initWorkflowCounters] new entry(ies) should be created in `limitCounters` collection after migration", 0,
        (long) counter.getValue());
    assertEquals(workflowCount, (long) counter.getValue());
  }
}