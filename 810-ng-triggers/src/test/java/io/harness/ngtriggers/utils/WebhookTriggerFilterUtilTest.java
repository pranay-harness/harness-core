package io.harness.ngtriggers.utils;

import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.CLOSED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.OPENED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.MERGE_REQUEST;
import static io.harness.ngtriggers.utils.WebhookTriggerFilterUtils.checkIfActionMatches;
import static io.harness.rule.OwnerRule.ADWAIT;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.PRWebhookEvent;
import io.harness.ngtriggers.beans.scm.PRWebhookEvent.PRWebhookEventBuilder;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes.WebhookBaseAttributesBuilder;
import io.harness.ngtriggers.beans.scm.WebhookEvent.Type;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData.WebhookPayloadDataBuilder;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookPayloadCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.expressions.TriggerExpressionEvaluator;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookTriggerFilterUtilTest extends CategoryTest {
  private String payload = "    {\n"
      + "\t\t\"object_kind\": \"merge_request\",\n"
      + "\t\t\"event_type\": \"merge_request\",\n"
      + "\t\t\"user\": {\n"
      + "\t\t  \"name\": \"charles grant\",\n"
      + "\t\t  \"username\": \"charles.grant\",\n"
      + "\t\t  \"avatar_url\": \"https://secure.gravatar.com/avatar/8e\",\n"
      + "\t\t  \"email\": \"cgrant@gmail.com\"\n"
      + "\t\t}\n"
      + "    } ";

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void parseEventTest() {
    int i = 0;
    TriggerExpressionEvaluator triggerExpressionEvaluator =
        WebhookTriggerFilterUtils.generatorPMSExpressionEvaluator(payload);
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.event_type>", triggerExpressionEvaluator))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.object_kind>", triggerExpressionEvaluator))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.user.name>", triggerExpressionEvaluator))
        .isEqualTo("charles grant");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.user.username>", triggerExpressionEvaluator))
        .isEqualTo("charles.grant");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.user.avatar_url>", triggerExpressionEvaluator))
        .isEqualTo("https://secure.gravatar.com/avatar/8e");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+eventPayload.user.email>", triggerExpressionEvaluator))
        .isEqualTo("cgrant@gmail.com");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void checkIfActionMatchesTest() {
    WebhookBaseAttributesBuilder baseAttributesBuilder = WebhookBaseAttributes.builder().action("open");
    PRWebhookEventBuilder prWebhookEventBuilder =
        PRWebhookEvent.builder().baseAttributes(baseAttributesBuilder.build());
    WebhookPayloadDataBuilder webhookPayloadDataBuilder =
        WebhookPayloadData.builder()
            .originalEvent(TriggerWebhookEvent.builder().payload(payload).build())
            .webhookEvent(prWebhookEventBuilder.build());

    List<WebhookAction> webhookActions = new ArrayList<>();
    webhookActions.add(OPENED);
    WebhookTriggerSpec webhookTriggerSpec = WebhookTriggerSpec.builder().actions(webhookActions).build();
    assertThat(checkIfActionMatches(webhookPayloadDataBuilder.build(), webhookTriggerSpec)).isTrue();

    webhookActions.clear();
    assertThat(checkIfActionMatches(webhookPayloadDataBuilder.build(), webhookTriggerSpec)).isTrue();
    webhookActions.add(CLOSED);
    assertThat(checkIfActionMatches(webhookPayloadDataBuilder.build(), webhookTriggerSpec)).isFalse();

    baseAttributesBuilder.action("close");
    webhookPayloadDataBuilder.webhookEvent(prWebhookEventBuilder.baseAttributes(baseAttributesBuilder.build()).build())
        .build();
    webhookActions.clear();
    webhookActions.add(CLOSED);
    assertThat(checkIfActionMatches(webhookPayloadDataBuilder.build(), webhookTriggerSpec)).isTrue();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void evaluateFilterConditionsTest() {
    WebhookTriggerSpec webhookTriggerSpec =
        WebhookTriggerSpec.builder()
            .actions(emptyList())
            .event(MERGE_REQUEST)
            .payloadConditions(Arrays.asList(
                WebhookPayloadCondition.builder().key("sourceBranch").operator("equals").value("stage").build(),
                WebhookPayloadCondition.builder().key("sourceBranch").operator("not equals").value("qa").build(),
                WebhookPayloadCondition.builder().key("targetBranch").operator("regex").value("^master$").build(),
                WebhookPayloadCondition.builder()
                    .key("<+eventPayload.event_type>")
                    .operator("in")
                    .value("pull_request, merge_request")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("<+eventPayload.object_kind>")
                    .operator("not in")
                    .value("push, package")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("<+eventPayload.user.name>")
                    .operator("starts with")
                    .value("charles")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("<+eventPayload.user.username>")
                    .operator("ends with")
                    .value("grant")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("<+eventPayload.user.avatar_url>")
                    .operator("contains")
                    .value("secure.gravatar.com")
                    .build()))
            .build();

    WebhookPayloadData webhookPayloadData =
        WebhookPayloadData.builder()
            .originalEvent(TriggerWebhookEvent.builder().payload(payload).build())
            .webhookEvent(PRWebhookEvent.builder()
                              .baseAttributes(WebhookBaseAttributes.builder().source("stage").target("master").build())
                              .build())
            .build();

    assertThat(WebhookTriggerFilterUtils.checkIfEventTypeMatches(Type.PR, webhookTriggerSpec.getEvent())).isTrue();
    assertThat(checkIfActionMatches(webhookPayloadData, webhookTriggerSpec)).isTrue();
    assertThat(WebhookTriggerFilterUtils.checkIfPayloadConditionsMatch(
                   webhookPayloadData, webhookTriggerSpec.getPayloadConditions()))
        .isTrue();
  }
}
