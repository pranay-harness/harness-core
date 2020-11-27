package io.harness.ngtriggers.mapper;

import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.CLOSED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.OPENED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.PULL_REQUEST;
import static io.harness.ngtriggers.beans.target.TargetType.PIPELINE;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.rule.Owner;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NGTriggerElementMapperTest extends CategoryTest {
  private String ngTriggerYaml;

  @Before
  public void setUp() throws IOException {
    ClassLoader classLoader = getClass().getClassLoader();
    String filename = "ng-trigger.yaml";
    ngTriggerYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(filename)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testToTriggerConfig() {
    NGTriggerConfig trigger = NGTriggerElementMapper.toTriggerConfig(ngTriggerYaml);
    assertThat(trigger).isNotNull();
    assertThat(trigger.getIdentifier()).isEqualTo("first_trigger");
    assertThat(trigger.getSource().getType()).isEqualTo(WEBHOOK);
    assertThat(trigger.getSource().getSpec()).isInstanceOfAny(WebhookTriggerConfig.class);

    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) trigger.getSource().getSpec();
    assertThat(webhookTriggerConfig.getType()).isEqualTo("Github");
    assertThat(webhookTriggerConfig.getSpec()).isNotNull();

    WebhookTriggerSpec webhookTriggerConfigSpec = webhookTriggerConfig.getSpec();
    assertThat(webhookTriggerConfigSpec.getEvent()).isEqualTo(PULL_REQUEST);
    assertThat(webhookTriggerConfigSpec.getActions()).containsExactlyInAnyOrder(OPENED, CLOSED);
    assertThat(webhookTriggerConfigSpec.getRepoUrl()).isEqualTo("https://github.com/test/myrepo");
    assertThat(webhookTriggerConfigSpec.getPathFilters()).containsExactlyInAnyOrder("path1", "path2");
    assertThat(webhookTriggerConfigSpec.getPayloadConditions()).isNotNull();
    assertThat(webhookTriggerConfigSpec.getPayloadConditions().size()).isEqualTo(3);

    Set<String> payloadConditionSet = webhookTriggerConfigSpec.getPayloadConditions()
                                          .stream()
                                          .map(webhookPayloadCondition
                                              -> new StringBuilder(128)
                                                     .append(webhookPayloadCondition.getKey())
                                                     .append(':')
                                                     .append(webhookPayloadCondition.getOperator())
                                                     .append(':')
                                                     .append(webhookPayloadCondition.getValue())
                                                     .toString())
                                          .collect(Collectors.toSet());

    assertThat(payloadConditionSet)
        .containsOnly("sourceBranch:equals:dev", "targetBranch:in:master, on-prem",
            "${pull_request.number}:regex:^pr-[0-9a-f]{7}$");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testToTriggerEntityFromYaml() {
    NGTriggerEntity ngTriggerEntity = NGTriggerElementMapper.toTriggerEntity("accId", "orgId", "projId", ngTriggerYaml);

    assertThat(ngTriggerEntity.getAccountId()).isEqualTo("accId");
    assertThat(ngTriggerEntity.getOrgIdentifier()).isEqualTo("orgId");
    assertThat(ngTriggerEntity.getProjectIdentifier()).isEqualTo("projId");
    assertThat(ngTriggerEntity.getYaml()).isEqualTo(ngTriggerYaml);
    assertThat(ngTriggerEntity.getIdentifier()).isEqualTo("first_trigger");
    assertThat(ngTriggerEntity.getName()).isEqualTo("first trigger");
    assertThat(ngTriggerEntity.getTargetType()).isEqualTo(PIPELINE);
    assertThat(ngTriggerEntity.getTargetIdentifier()).isEqualTo("myPipeline");

    NGTriggerMetadata metadata = ngTriggerEntity.getMetadata();
    assertThat(metadata).isNotNull();
    assertThat(metadata.getWebhook()).isNotNull();
    assertThat(metadata.getWebhook().getRepoURL()).isEqualTo("https://github.com/test/myrepo");
    assertThat(metadata.getWebhook().getType()).isEqualTo("Github");
  }
}
