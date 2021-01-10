package io.harness.ngtriggers.expressions;

import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TriggerExpressionEvaluatorTest extends CategoryTest {
  String json = "{\n"
      + "  \"action\": \"opened\",\n"
      + "  \"number\": 1,\n"
      + "  \"pull_request\": {\n"
      + "    \"id\": 526274089,\n"
      + "    \"assignee\": null,\n"
      + "    \"assignees\": [\n"
      + "       {\"name\": \"wings\"},\n"
      + "       {\"name\": \"harness\"}\n"
      + "    ]\n"
      + "  }\n"
      + "}";
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testMapWebhookEventToTriggers() {
    TriggerExpressionEvaluator triggerExpressionEvaluator = new TriggerExpressionEvaluator(json);
    assertThat(triggerExpressionEvaluator.renderExpression("<+eventPayload.pull_request.assignees[0].name>"))
        .isEqualTo("wings");
    assertThat(triggerExpressionEvaluator.renderExpression("<+eventPayload.pull_request.assignees[1].name>"))
        .isEqualTo("harness");
    int i = 0;
  }
}