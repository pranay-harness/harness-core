/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngtriggers.mapper;

import static io.harness.ngtriggers.beans.source.NGTriggerType.SCHEDULED;
import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.CLOSED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookAction.OPENED;
import static io.harness.ngtriggers.beans.source.webhook.WebhookEvent.PULL_REQUEST;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.CustomWebhookTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec;
import io.harness.rule.Owner;
import io.harness.utils.YamlPipelineUtils;

import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class NGTriggerElementMapperTest extends CategoryTest {
  private String ngCustomTriggerYaml;
  private String ngTriggerGitConnYaml;
  private String ngTriggerCronYaml;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    ClassLoader classLoader = getClass().getClassLoader();

    String gitYaml = "ng-trigger-v0.yaml";
    ngTriggerGitConnYaml =
        Resources.toString(Objects.requireNonNull(classLoader.getResource(gitYaml)), StandardCharsets.UTF_8);

    String fileNameForCustomPayloadTrigger = "ng-custom-trigger-v0.yaml";
    ngCustomTriggerYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(fileNameForCustomPayloadTrigger)), StandardCharsets.UTF_8);

    String fileNgTriggerCronYaml = "ng-trigger-cron-v0.yaml";
    ngTriggerCronYaml = Resources.toString(
        Objects.requireNonNull(classLoader.getResource(fileNgTriggerCronYaml)), StandardCharsets.UTF_8);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testToTriggerConfig() throws Exception {
    NGTriggerConfig trigger = YamlPipelineUtils.read(ngTriggerGitConnYaml, NGTriggerConfig.class);

    assertThat(trigger).isNotNull();
    assertThat(trigger.getIdentifier()).isEqualTo("first_trigger");
    assertThat(trigger.getSource().getType()).isEqualTo(WEBHOOK);
    assertThat(trigger.getSource().getSpec()).isInstanceOfAny(WebhookTriggerConfig.class);

    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) trigger.getSource().getSpec();
    assertThat(webhookTriggerConfig.getType()).isEqualTo("GITHUB");
    assertThat(webhookTriggerConfig.getSpec()).isNotNull();

    WebhookTriggerSpec webhookTriggerConfigSpec = webhookTriggerConfig.getSpec();
    assertThat(webhookTriggerConfigSpec.getEvent()).isEqualTo(PULL_REQUEST);
    assertThat(webhookTriggerConfigSpec.getActions()).containsExactlyInAnyOrder(OPENED, CLOSED);
    assertThat(webhookTriggerConfigSpec.getPathFilters()).containsExactlyInAnyOrder("path1", "path2");
    assertThat(webhookTriggerConfigSpec.getJexlCondition()).isEqualTo("true");
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
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testToTriggerConfigForCustomPayloadTrigger() throws Exception {
    NGTriggerConfig trigger = YamlPipelineUtils.read(ngCustomTriggerYaml, NGTriggerConfig.class);
    assertCustomTrigger(trigger);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void testToTriggerConfigForCronTrigger() throws Exception {
    NGTriggerConfig trigger = YamlPipelineUtils.read(ngTriggerCronYaml, NGTriggerConfig.class);

    assertThat(trigger).isNotNull();
    assertThat(trigger.getIdentifier()).isEqualTo("cronTrigger");
    assertThat(trigger.getSource().getType()).isEqualTo(SCHEDULED);
    assertThat(trigger.getSource().getSpec()).isInstanceOfAny(ScheduledTriggerConfig.class);

    ScheduledTriggerConfig scheduledTriggerConfig = (ScheduledTriggerConfig) trigger.getSource().getSpec();
    assertThat(scheduledTriggerConfig.getType()).isEqualTo("Cron");
    assertThat(scheduledTriggerConfig.getSpec()).isNotNull();

    CronTriggerSpec cronTriggerSpec = (CronTriggerSpec) scheduledTriggerConfig.getSpec();
    assertThat(cronTriggerSpec.getExpression()).isEqualTo("20 4 * * *");
  }

  private void assertCustomTrigger(NGTriggerConfig trigger) {
    assertThat(trigger).isNotNull();
    assertThat(trigger.getIdentifier()).isEqualTo("customPayload");
    assertThat(trigger.getSource().getType()).isEqualTo(WEBHOOK);
    assertThat(trigger.getSource().getSpec()).isInstanceOfAny(WebhookTriggerConfig.class);

    WebhookTriggerConfig webhookTriggerConfig = (WebhookTriggerConfig) trigger.getSource().getSpec();
    assertThat(webhookTriggerConfig.getType()).isEqualTo("CUSTOM");
    assertThat(webhookTriggerConfig.getSpec()).isNotNull();

    CustomWebhookTriggerSpec customWebhookTriggerSpec = (CustomWebhookTriggerSpec) webhookTriggerConfig.getSpec();
    assertThat(customWebhookTriggerSpec.getPayloadConditions()).isNotNull();
    assertThat(customWebhookTriggerSpec.getPayloadConditions().size()).isEqualTo(1);

    Set<String> payloadConditionSet = customWebhookTriggerSpec.getPayloadConditions()
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

    assertThat(payloadConditionSet).containsOnly("<+trigger.payload.project.team>:in:cd, ci");
  }
}
