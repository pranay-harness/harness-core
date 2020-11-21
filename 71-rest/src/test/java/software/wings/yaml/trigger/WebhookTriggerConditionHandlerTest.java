package software.wings.yaml.trigger;

import static io.harness.rule.OwnerRule.HARSH;

import static software.wings.beans.trigger.GithubAction.CLOSED;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.trigger.WebHookTriggerCondition;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookTriggerConditionHandlerTest extends CategoryTest {
  @Inject private WebhookTriggerConditionHandler webhookTriggerConditionHandler = new WebhookTriggerConditionHandler();

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void toYaml() {
    WebHookTriggerCondition webHookTriggerCondition =
        WebHookTriggerCondition.builder().actions(asList(CLOSED)).branchRegex("abc").build();

    WebhookEventTriggerConditionYaml webhookEventTriggerConditionYaml =
        webhookTriggerConditionHandler.toYaml(webHookTriggerCondition, "APP_ID");

    assertThat(webhookEventTriggerConditionYaml.getBranchRegex().equals(webHookTriggerCondition.getBranchRegex()))
        .isTrue();
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
    WebHookTriggerCondition webHookTriggerCondition =
        webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml);

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
    WebHookTriggerCondition webHookTriggerCondition =
        webhookTriggerConditionHandler.fromYAML(webhookEventTriggerConditionYaml);

    assertThat(webhookEventTriggerConditionYaml.getBranchRegex().equals(webHookTriggerCondition.getBranchRegex()))
        .isTrue();
  }
}
