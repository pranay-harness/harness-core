package io.harness.ngtriggers.utils;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.constants.Constants.X_HARNESS_TRIGGER_ID;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.CLOSED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.OPENED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.MERGE_REQUEST;
import static io.harness.ngtriggers.utils.WebhookTriggerFilterUtils.checkIfActionMatches;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.HeaderConfig;
import io.harness.beans.PRWebhookEvent;
import io.harness.beans.PRWebhookEvent.PRWebhookEventBuilder;
import io.harness.beans.WebhookBaseAttributes;
import io.harness.beans.WebhookBaseAttributes.WebhookBaseAttributesBuilder;
import io.harness.beans.WebhookEvent.Type;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData.WebhookPayloadDataBuilder;
import io.harness.ngtriggers.beans.source.webhook.CustomWebhookTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GithubTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GitlabTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.ngtriggers.expressions.TriggerExpressionEvaluator;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
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
        WebhookTriggerFilterUtils.generatorPMSExpressionEvaluator(null, emptyList(), payload);
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.event_type>", triggerExpressionEvaluator))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.object_kind>", triggerExpressionEvaluator))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.user.name>", triggerExpressionEvaluator))
        .isEqualTo("charles grant");
    assertThat(
        WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.user.username>", triggerExpressionEvaluator))
        .isEqualTo("charles.grant");
    assertThat(
        WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.user.avatar_url>", triggerExpressionEvaluator))
        .isEqualTo("https://secure.gravatar.com/avatar/8e");
    assertThat(WebhookTriggerFilterUtils.readFromPayload("<+trigger.payload.user.email>", triggerExpressionEvaluator))
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
    WebhookTriggerSpec webhookTriggerSpec = GithubTriggerSpec.builder().actions(webhookActions).build();
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
    WebhookTriggerSpec webhookTriggerSpec = getGitLabTriggerSpec();

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

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void evaluateFilterConditionsForCustomPayloadWebhookTest() {
    WebhookTriggerSpec webhookTriggerSpec = getCustomWebhookTriggerSpec();

    List<HeaderConfig> headerConfigList =
        Arrays.asList(HeaderConfig.builder().key("X-GITHUB-EVENT").values(Arrays.asList("push")).build(),
            HeaderConfig.builder().key(X_HARNESS_TRIGGER_ID).values(Arrays.asList("customertriggerspec")).build());

    assertThat(WebhookTriggerFilterUtils.checkIfCustomHeaderConditionsMatch(headerConfigList, webhookTriggerSpec))
        .isFalse();
  }

  private CustomWebhookTriggerSpec getCustomWebhookTriggerSpec() {
    return CustomWebhookTriggerSpec.builder()
        .headerConditions(Arrays.asList(WebhookCondition.builder()
                                            .key("X-HARNESS-TRIGGER-IDENTIFIER")
                                            .operator("equals")
                                            .value("customertriggerspec")
                                            .build(),
            WebhookCondition.builder().key("X-GITHUB-EVENT").operator("in").value("push, pull_request").build()))
        .build();
  }

  private GitlabTriggerSpec getGitLabTriggerSpec() {
    return GitlabTriggerSpec.builder()
        .actions(emptyList())
        .event(MERGE_REQUEST)
        .payloadConditions(
            Arrays.asList(WebhookCondition.builder().key("sourceBranch").operator("equals").value("stage").build(),
                WebhookCondition.builder().key("sourceBranch").operator("not equals").value("qa").build(),
                WebhookCondition.builder().key("targetBranch").operator("regex").value("^master$").build(),
                WebhookCondition.builder()
                    .key("<+trigger.payload.event_type>")
                    .operator("in")
                    .value("pull_request, merge_request")
                    .build(),
                WebhookCondition.builder()
                    .key("<+trigger.payload.object_kind>")
                    .operator("not in")
                    .value("push, package")
                    .build(),
                WebhookCondition.builder()
                    .key("<+trigger.payload.user.name>")
                    .operator("starts with")
                    .value("charles")
                    .build(),
                WebhookCondition.builder()
                    .key("<+trigger.payload.user.username>")
                    .operator("ends with")
                    .value("grant")
                    .build(),
                WebhookCondition.builder()
                    .key("<+trigger.payload.user.avatar_url>")
                    .operator("contains")
                    .value("secure.gravatar.com")
                    .build()))
        .build();
  }
}
