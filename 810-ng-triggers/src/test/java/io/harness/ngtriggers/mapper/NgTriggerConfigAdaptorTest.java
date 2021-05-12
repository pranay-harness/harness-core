package io.harness.ngtriggers.mapper;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.ngtriggers.beans.source.NGTriggerType.SCHEDULED;
import static io.harness.ngtriggers.beans.source.NGTriggerType.WEBHOOK;
import static io.harness.rule.OwnerRule.ADWAIT;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.NGTriggerConfig;
import io.harness.ngtriggers.beans.config.NGTriggerConfigV2;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.source.NGTriggerSource;
import io.harness.ngtriggers.beans.source.NGTriggerSourceV2;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;
import io.harness.ngtriggers.beans.source.scheduled.CronTriggerSpec;
import io.harness.ngtriggers.beans.source.scheduled.ScheduledTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.AwsCodeCommitTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.BitbucketTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.CustomWebhookTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GitRepoSpec;
import io.harness.ngtriggers.beans.source.webhook.GithubTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.GitlabTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.WebhookAction;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookEvent;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerConfig;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerConfigV2;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.AwsCodeCommitSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.awscodecommit.event.AwsCodeCommitTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.BitbucketSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.action.BitbucketPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.bitbucket.event.BitbucketTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.custom.CustomTriggerSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.GithubSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubIssueCommentAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.action.GithubPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.github.event.GithubTriggerEvent;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.GitlabSpec;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.action.GitlabPRAction;
import io.harness.ngtriggers.beans.source.webhook.v2.gitlab.event.GitlabTriggerEvent;
import io.harness.ngtriggers.beans.target.NGTriggerTarget;
import io.harness.ngtriggers.beans.target.TargetType;
import io.harness.ngtriggers.beans.target.pipeline.PipelineTargetSpec;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class NgTriggerConfigAdaptorTest extends CategoryTest {
  private static final String ACC_ID = "acc";
  private static final String ORG_ID = "org";
  private static final String PROJ_ID = "proj";
  private static final String PIPELINE_ID = "pipeline";
  private static final String TRIGGER_ID = "trigger";
  private static final String NAME = "name";
  private static final String INPUT_YAML = "input_yaml";
  private static final String CON_REF = "conn";
  private static final String REPO_NAME = "repo";
  public static final String EXPRESSION = "* * * *";
  private NGTriggerEntity ngTriggerEntity;
  private List<WebhookCondition> payloadConditions;
  private List<WebhookCondition> headerConditions;
  private static final String JEXL = "1==1";

  @Before
  public void setUp() {
    ngTriggerEntity = NGTriggerEntity.builder()
                          .accountId(ACC_ID)
                          .orgIdentifier(ORG_ID)
                          .projectIdentifier(PROJ_ID)
                          .targetType(TargetType.PIPELINE)
                          .targetIdentifier(PIPELINE_ID)
                          .identifier(TRIGGER_ID)
                          .build();

    payloadConditions = asList(WebhookCondition.builder().key("key").operator("equals").value("val").build());
    headerConditions = asList(WebhookCondition.builder().key("header").operator("equals").value("val").build());
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertScheduledTriggerFromV0ToV2() {
    ngTriggerEntity.setType(SCHEDULED);
    ScheduledTriggerConfig triggerConfig = ScheduledTriggerConfig.builder()
                                               .type("Cron")
                                               .spec(CronTriggerSpec.builder().expression(EXPRESSION).build())
                                               .build();
    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder().type(SCHEDULED).spec(triggerConfig).build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(SCHEDULED);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(ScheduledTriggerConfig.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    ScheduledTriggerConfig scheduledTriggerConfig = (ScheduledTriggerConfig) ngTriggerSpecV2;
    assertThat(scheduledTriggerConfig).isEqualTo(triggerConfig);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertGithubPRTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PULL_REQUEST)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .actions(new ArrayList<>(
                                                WebhookAction.getGithubActionForEvent(WebhookEvent.PULL_REQUEST)))
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.PULL_REQUEST);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.PULL_REQUEST);
    assertThat(githubSpec.fetchGitAware().fetchActions()).containsAll(asList(GithubPRAction.values()));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertGithubIssueCommentTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.ISSUE_COMMENT)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .actions(new ArrayList<>(
                                                WebhookAction.getGithubActionForEvent(WebhookEvent.ISSUE_COMMENT)))
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.ISSUE_COMMENT);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.ISSUE_COMMENT);
    assertThat(githubSpec.fetchGitAware().fetchActions()).containsAll(asList(GithubIssueCommentAction.values()));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertGithubPushTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITHUB")
                                  .spec(GithubTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITHUB);
    assertThat(GithubSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GithubSpec githubSpec = (GithubSpec) webhookTriggerConfigV2.getSpec();
    assertThat(githubSpec.getType()).isEqualTo(GithubTriggerEvent.PUSH);
    assertThat(githubSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(githubSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(githubSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(githubSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(githubSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(githubSpec.fetchGitAware().fetchEvent()).isEqualTo(GithubTriggerEvent.PUSH);
    assertThat(githubSpec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertGitlabPRTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITLAB")
                                  .spec(GitlabTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.MERGE_REQUEST)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .actions(new ArrayList<>(
                                                WebhookAction.getGitLabActionForEvent(WebhookEvent.MERGE_REQUEST)))
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITLAB);
    assertThat(GitlabSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfigV2.getSpec();
    assertThat(gitlabSpec.getType()).isEqualTo(GitlabTriggerEvent.MERGE_REQUEST);
    assertThat(gitlabSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(gitlabSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(gitlabSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(gitlabSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(gitlabSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(gitlabSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(gitlabSpec.fetchGitAware().fetchEvent()).isEqualTo(GitlabTriggerEvent.MERGE_REQUEST);
    assertThat(gitlabSpec.fetchGitAware().fetchActions()).containsAll(asList(GitlabPRAction.values()));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertBitbucketPushTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("BITBUCKET")
                                  .spec(BitbucketTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.BITBUCKET);
    assertThat(BitbucketSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfigV2.getSpec();
    assertThat(bitbucketSpec.getType()).isEqualTo(BitbucketTriggerEvent.PUSH);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(bitbucketSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(bitbucketSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(bitbucketSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(bitbucketSpec.fetchGitAware().fetchEvent()).isEqualTo(BitbucketTriggerEvent.PUSH);
    assertThat(bitbucketSpec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertBitbucketPRTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("BITBUCKET")
                                  .spec(BitbucketTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PULL_REQUEST)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .actions(new ArrayList<>(
                                                WebhookAction.getBitbucketActionForEvent(WebhookEvent.PULL_REQUEST)))
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.BITBUCKET);
    assertThat(BitbucketSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    BitbucketSpec bitbucketSpec = (BitbucketSpec) webhookTriggerConfigV2.getSpec();
    assertThat(bitbucketSpec.getType()).isEqualTo(BitbucketTriggerEvent.PULL_REQUEST);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(bitbucketSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(bitbucketSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(bitbucketSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(bitbucketSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(bitbucketSpec.fetchGitAware().fetchEvent()).isEqualTo(BitbucketTriggerEvent.PULL_REQUEST);
    assertThat(bitbucketSpec.fetchGitAware().fetchActions()).containsAll(asList(BitbucketPRAction.values()));
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertGitlabPushTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("GITLAB")
                                  .spec(GitlabTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.GITLAB);
    assertThat(GitlabSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    GitlabSpec gitlabSpec = (GitlabSpec) webhookTriggerConfigV2.getSpec();
    assertThat(gitlabSpec.getType()).isEqualTo(GitlabTriggerEvent.PUSH);
    assertThat(gitlabSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(gitlabSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(gitlabSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(gitlabSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(gitlabSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(gitlabSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(gitlabSpec.fetchGitAware().fetchEvent()).isEqualTo(GitlabTriggerEvent.PUSH);
    assertThat(gitlabSpec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertAwsCodeCommitPushTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("AWSCODECOMMIT")
                                  .spec(AwsCodeCommitTriggerSpec.builder()
                                            .gitRepoSpec(
                                                GitRepoSpec.builder().identifier(CON_REF).repoName(REPO_NAME).build())
                                            .event(WebhookEvent.PUSH)
                                            .payloadConditions(payloadConditions)
                                            .jexlCondition(JEXL)
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.AWS_CODECOMMIT);
    assertThat(AwsCodeCommitSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    AwsCodeCommitSpec awsCodeCommitSpec = (AwsCodeCommitSpec) webhookTriggerConfigV2.getSpec();
    assertThat(awsCodeCommitSpec.getType()).isEqualTo(AwsCodeCommitTriggerEvent.PUSH);
    assertThat(awsCodeCommitSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(awsCodeCommitSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);

    assertThat(awsCodeCommitSpec.fetchGitAware().fetchRepoName()).isEqualTo(REPO_NAME);
    assertThat(awsCodeCommitSpec.fetchGitAware().fetchConnectorRef()).isEqualTo(CON_REF);
    assertThat(awsCodeCommitSpec.fetchGitAware().fetchAutoAbortPreviousExecutions()).isFalse();
    assertThat(awsCodeCommitSpec.fetchGitAware().fetchEvent()).isEqualTo(AwsCodeCommitTriggerEvent.PUSH);
    assertThat(awsCodeCommitSpec.fetchGitAware().fetchActions()).isEmpty();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testConvertCustomTriggerFromV0ToV2() {
    ngTriggerEntity.setType(WEBHOOK);

    NGTriggerConfig ngTriggerConfig =
        NGTriggerConfig.builder()
            .enabled(true)
            .name(NAME)
            .identifier(TRIGGER_ID)
            .target(NGTriggerTarget.builder()
                        .targetIdentifier(PIPELINE_ID)
                        .spec(PipelineTargetSpec.builder().runtimeInputYaml(INPUT_YAML).build())
                        .build())
            .source(NGTriggerSource.builder()
                        .type(WEBHOOK)
                        .spec(WebhookTriggerConfig.builder()
                                  .type("CUSTOM")
                                  .spec(CustomWebhookTriggerSpec.builder()
                                            .payloadConditions(payloadConditions)
                                            .headerConditions(headerConditions)
                                            .jexlCondition(JEXL)
                                            .build())
                                  .build())
                        .build())
            .build();

    NGTriggerConfigV2 ngTriggerConfigV2 = NgTriggerConfigAdaptor.convertFromV0ToV2(ngTriggerConfig, ngTriggerEntity);
    assertRootLevelProperties(ngTriggerConfigV2);

    NGTriggerSourceV2 ngTriggerSourceV2 = ngTriggerConfigV2.getSource();
    assertThat(ngTriggerSourceV2).isNotNull();
    assertThat(ngTriggerSourceV2.getType()).isEqualTo(WEBHOOK);
    NGTriggerSpecV2 ngTriggerSpecV2 = ngTriggerSourceV2.getSpec();
    assertThat(WebhookTriggerConfigV2.class.isAssignableFrom(ngTriggerSpecV2.getClass())).isTrue();
    WebhookTriggerConfigV2 webhookTriggerConfigV2 = (WebhookTriggerConfigV2) ngTriggerSpecV2;
    assertThat(webhookTriggerConfigV2.getType()).isEqualTo(WebhookTriggerType.CUSTOM);
    assertThat(CustomTriggerSpec.class.isAssignableFrom(webhookTriggerConfigV2.getSpec().getClass())).isTrue();
    CustomTriggerSpec customTriggerSpec = (CustomTriggerSpec) webhookTriggerConfigV2.getSpec();
    assertThat(customTriggerSpec.fetchPayloadAware().fetchPayloadConditions()).isEqualTo(payloadConditions);
    assertThat(customTriggerSpec.fetchPayloadAware().fetchHeaderConditions()).isEqualTo(headerConditions);
    assertThat(customTriggerSpec.fetchPayloadAware().fetchJexlCondition()).isEqualTo(JEXL);
  }

  private void assertRootLevelProperties(NGTriggerConfigV2 ngTriggerConfigV2) {
    assertThat(ngTriggerConfigV2).isNotNull();
    assertThat(ngTriggerConfigV2.getIdentifier()).isEqualTo(TRIGGER_ID);
    assertThat(ngTriggerConfigV2.getEnabled()).isTrue();
    assertThat(ngTriggerConfigV2.getInputYaml()).isEqualTo(INPUT_YAML);
    assertThat(ngTriggerConfigV2.getPipelineIdentifier()).isEqualTo(PIPELINE_ID);
    assertThat(ngTriggerConfigV2.getOrgIdentifier()).isEqualTo(ORG_ID);
    assertThat(ngTriggerConfigV2.getProjectIdentifier()).isEqualTo(PROJ_ID);
    assertThat(ngTriggerConfigV2.getName()).isEqualTo(NAME);
  }
}
