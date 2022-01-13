/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.INDER;

import static software.wings.beans.trigger.GithubAction.CLOSED;
import static software.wings.beans.trigger.WebhookSource.GITHUB;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joor.Reflect.on;
import static org.mockito.Mockito.when;

import io.harness.beans.EncryptedData;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class WebhookTriggerConditionHandlerTest extends WingsBaseTest {
  @Inject @InjectMocks private WebhookTriggerConditionHandler webhookTriggerConditionHandler;
  @Inject private YamlHelper yamlHelper;

  @Mock private FeatureFlagService featureFlagService;
  @Mock private AppService appService;
  @Mock private SecretManager secretManager;

  private static final String ACCOUNTID = "accountId";
  private static final String SECRET = "secret";

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void toYaml() {
    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder().actions(asList(CLOSED)).branchRegex("abc").build();
    when(appService.getAccountIdByAppId("APP_ID")).thenReturn(ACCOUNTID);
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(false);

    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml =
        webhookTriggerConditionHandler.toYaml(webHookTriggerCondition, "APP_ID");

    assertThat(webhookEventTriggerConditionYaml.getBranchRegex().equals(webHookTriggerCondition.getBranchRegex()))
        .isTrue();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void toYaml_FfOn() {
    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder().webHookSecret(SECRET).webhookSource(GITHUB).build();
    when(appService.getAccountIdByAppId("APP_ID")).thenReturn(ACCOUNTID);
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(true);
    when(secretManager.getEncryptedYamlRef(ACCOUNTID, SECRET)).thenReturn("safeharness:secret");

    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml =
        webhookTriggerConditionHandler.toYaml(webHookTriggerCondition, "APP_ID");

    assertThat(webhookEventTriggerConditionYaml.getRepositoryType()).isEqualTo(GITHUB.name());
    assertThat(webhookEventTriggerConditionYaml.getWebhookSecret()).isEqualTo("safeharness:secret");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void upsertFromYaml() {
    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml = WebhookEventTriggerConditionYaml.builder()
                                                                            .action(asList("closed"))
                                                                            .branchRegex("abc")
                                                                            .repositoryType("GITHUB")
                                                                            .build();
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(false);
    WebHookTriggerCondition webHookTriggerCondition =
        webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml, ACCOUNTID);

    assertThat(webhookEventTriggerConditionYaml.getBranchRegex().equals(webHookTriggerCondition.getBranchRegex()))
        .isTrue();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void upsertFromYamlForBitBucket() {
    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml = WebhookEventTriggerConditionYaml.builder()
                                                                            .action(asList("repo:push"))
                                                                            .branchRegex("abc")
                                                                            .repositoryType("BITBUCKET")
                                                                            .build();
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(false);
    WebHookTriggerCondition webHookTriggerCondition =
        webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml, ACCOUNTID);

    assertThat(webhookEventTriggerConditionYaml.getBranchRegex().equals(webHookTriggerCondition.getBranchRegex()))
        .isTrue();
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void upsertFromYamlForBitBucket_WithWebHookSecret() {
    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml = WebhookEventTriggerConditionYaml.builder()
                                                                            .action(asList("repo:push"))
                                                                            .branchRegex("abc")
                                                                            .repositoryType("BITBUCKET")
                                                                            .webhookSecret(SECRET)
                                                                            .build();
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(true);

    assertThatThrownBy(() -> webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml, ACCOUNTID))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("WebHook Secret is only supported with Github repository");
  }

  @Test
  @Owner(developers = INDER)
  @Category(UnitTests.class)
  public void upsertFromYamlForGithub_WithWebHookSecret() {
    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml = WebhookEventTriggerConditionYaml.builder()
                                                                            .action(asList("closed"))
                                                                            .branchRegex("abc")
                                                                            .repositoryType("GITHUB")
                                                                            .webhookSecret("safeharness:secret")
                                                                            .build();
    when(featureFlagService.isEnabled(FeatureName.GITHUB_WEBHOOK_AUTHENTICATION, ACCOUNTID)).thenReturn(true);
    when(secretManager.getEncryptedDataFromYamlRef(SECRET, ACCOUNTID))
        .thenReturn(EncryptedData.builder().uuid(SECRET).build());
    on(yamlHelper).set("secretManager", secretManager);

    WebHookTriggerCondition webHookTriggerCondition =
        webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml, ACCOUNTID);
    assertThat(webHookTriggerCondition.getWebhookSource()).isEqualTo(GITHUB);
    assertThat(webHookTriggerCondition.getWebHookSecret()).isEqualTo(SECRET);
  }
}
