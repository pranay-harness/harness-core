package io.harness.ngtriggers.utils;

import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.MERGE_REQUEST;
import static io.harness.rule.OwnerRule.ADWAIT;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.PRWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookEvent.Type;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.beans.source.webhook.WebhookPayloadCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Map;

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

  String t = "pipeline:\n"
      + "    identifier: T1\n"
      + "    stages:\n"
      + "      - stage:\n"
      + "          identifier: S1\n"
      + "          type: Deployment\n"
      + "          spec:\n"
      + "            service:\n"
      + "              serviceDefinition:\n"
      + "                type: Kubernetes\n"
      + "                spec:\n"
      + "                  manifests:\n"
      + "                    - manifest:\n"
      + "                        identifier: manifestId\n"
      + "                        type: K8sManifest\n"
      + "                        spec:\n"
      + "                          store:\n"
      + "                            type: Git\n"
      + "                            spec:\n"
      + "                              branch: triggerTest\n"
      + "            infrastructure:\n"
      + "              infrastructureDefinition:\n"
      + "                type: KubernetesDirect\n"
      + "                spec:\n"
      + "                  releaseName: adwait-1001009\n";

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void parseEventTest() {
    int i = 0;
    Map<String, Object> context = WebhookTriggerFilterUtil.generateContext(payload);
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.event_type}", context))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.object_kind}", context))
        .isEqualTo("merge_request");
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.user.name}", context))
        .isEqualTo("charles grant");
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.user.username}", context))
        .isEqualTo("charles.grant");
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.user.avatar_url}", context))
        .isEqualTo("https://secure.gravatar.com/avatar/8e");
    assertThat(WebhookTriggerFilterUtil.readFromPayload("${eventPayload.user.email}", context))
        .isEqualTo("cgrant@gmail.com");
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
                    .key("${eventPayload.event_type}")
                    .operator("in")
                    .value("pull_request, merge_request")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("${eventPayload.object_kind}")
                    .operator("not in")
                    .value("push, package")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("${eventPayload.user.name}")
                    .operator("starts with")
                    .value("charles")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("${eventPayload.user.username}")
                    .operator("ends with")
                    .value("grant")
                    .build(),
                WebhookPayloadCondition.builder()
                    .key("${eventPayload.user.avatar_url}")
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

    assertThat(WebhookTriggerFilterUtil.checkIfEventTypeMatches(Type.PR, webhookTriggerSpec.getEvent())).isTrue();
    assertThat(WebhookTriggerFilterUtil.checkIfActionMatches(webhookPayloadData, webhookTriggerSpec)).isTrue();
    assertThat(WebhookTriggerFilterUtil.checkIfPayloadConditionsMatch(
                   webhookPayloadData, webhookTriggerSpec.getPayloadConditions()))
        .isTrue();
  }
}
