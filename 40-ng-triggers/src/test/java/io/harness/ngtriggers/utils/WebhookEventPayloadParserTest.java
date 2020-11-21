package io.harness.ngtriggers.utils;

import static io.harness.ngtriggers.beans.scm.WebhookEvent.Type.PR;
import static io.harness.rule.OwnerRule.ADWAIT;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.PRWebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookBaseAttributes;
import io.harness.ngtriggers.beans.scm.WebhookEvent;
import io.harness.ngtriggers.beans.scm.WebhookGitUser;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.product.ci.scm.proto.Action;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.Reference;
import io.harness.product.ci.scm.proto.Repository;
import io.harness.product.ci.scm.proto.User;
import io.harness.rule.Owner;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Spy;

public class WebhookEventPayloadParserTest extends CategoryTest {
  @Spy WebhookEventPayloadParser webhookEventPayloadParser;
  private TriggerWebhookEvent triggerWebhookEvent;
  private ParseWebhookResponse parseWebhookResponse;
  @Before
  public void setUp() throws IOException {
    initMocks(this);

    triggerWebhookEvent =
        TriggerWebhookEvent.builder()
            .headers(Arrays.asList(
                HeaderConfig.builder().key("content-type").values(Arrays.asList("application/json")).build(),
                HeaderConfig.builder().key("X-GitHub-Event").values(Arrays.asList("someValue")).build()))
            .payload("{a: b}")
            .build();

    parseWebhookResponse = ParseWebhookResponse.newBuilder()
                               .setPr(PullRequestHook.newBuilder()
                                          .setAction(Action.OPEN)
                                          .setSender(User.newBuilder()
                                                         .setName("uname")
                                                         .setEmail("userEmail")
                                                         .setLogin("userLogin")
                                                         .setAvatar("userAvtar")
                                                         .build())
                                          .setPr(PullRequest.newBuilder()
                                                     .setSource("sourceBranch")
                                                     .setTarget("targetBranch")
                                                     .setLink("prLink")
                                                     .setNumber(123)
                                                     .setTitle("prTitle")
                                                     .setClosed(false)
                                                     .setMerged(false)
                                                     .setBase(Reference.newBuilder().setSha("beforeSha").build())
                                                     .setSha("afterSha")
                                                     .setRef("prRef")
                                                     .setAuthor(User.newBuilder()
                                                                    .setName("uname")
                                                                    .setEmail("userEmail")
                                                                    .setLogin("userLogin")
                                                                    .setAvatar("userAvtar")
                                                                    .build())
                                                     .build())
                                          .setRepo(Repository.newBuilder()
                                                       .setName("myRepo")
                                                       .setNamespace("myNamespace")
                                                       .setLink("myLink")
                                                       .setBranch("master")
                                                       .setClone("https://myRepo")
                                                       .setCloneSsh("git@myRepo")
                                                       .build())
                                          .build())
                               .build();
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void parseEventTest() {
    doReturn(parseWebhookResponse).when(webhookEventPayloadParser).invokeScmService(any());
    WebhookPayloadData webhookPayloadData = webhookEventPayloadParser.parseEvent(triggerWebhookEvent);

    assertThat(webhookPayloadData).isNotNull();
    assertThat(webhookPayloadData.getOriginalEvent()).isEqualTo(triggerWebhookEvent);

    assertRepo(webhookPayloadData.getRepository());
    assertGitUser(webhookPayloadData.getWebhookGitUser());
    assertPullEventData(webhookPayloadData.getWebhookEvent());
  }

  private void assertPullEventData(WebhookEvent webhookEvent) {
    assertThat(webhookEvent).isNotNull();
    assertThat(webhookEvent.getType()).isEqualTo(PR);

    PRWebhookEvent prWebhookEvent = (PRWebhookEvent) webhookEvent;
    assertThat(prWebhookEvent.getSourceBranch()).isEqualTo("sourceBranch");
    assertThat(prWebhookEvent.getTargetBranch()).isEqualTo("targetBranch");
    assertThat(prWebhookEvent.getPullRequestLink()).isEqualTo("prLink");
    assertThat(prWebhookEvent.getPullRequestId()).isEqualTo(123);
    assertThat(prWebhookEvent.getTitle()).isEqualTo("prTitle");
    assertThat(prWebhookEvent.isClosed()).isFalse();
    assertThat(prWebhookEvent.isMerged()).isFalse();

    WebhookBaseAttributes baseAttributes = prWebhookEvent.getBaseAttributes();
    assertThat(baseAttributes.getAction()).isEqualTo("open");
    assertThat(baseAttributes.getLink()).isEqualTo("prLink");
    assertThat(baseAttributes.getAfter()).isEqualTo("afterSha");
    assertThat(baseAttributes.getBefore()).isEqualTo("beforeSha");
    assertThat(baseAttributes.getRef()).isEqualTo("prRef");
    assertThat(baseAttributes.getSource()).isEqualTo("sourceBranch");
    assertThat(baseAttributes.getTarget()).isEqualTo("targetBranch");
    assertThat(baseAttributes.getAuthorLogin()).isEqualTo("userLogin");
    assertThat(baseAttributes.getAuthorName()).isEqualTo("uname");
    assertThat(baseAttributes.getAuthorEmail()).isEqualTo("userEmail");
    assertThat(baseAttributes.getSender()).isEqualTo("userLogin");
  }

  private void assertGitUser(WebhookGitUser webhookGitUser) {
    assertThat(webhookGitUser).isNotNull();
    assertThat(webhookGitUser.getName()).isEqualTo("uname");
    assertThat(webhookGitUser.getEmail()).isEqualTo("userEmail");
    assertThat(webhookGitUser.getGitId()).isEqualTo("userLogin");
    assertThat(webhookGitUser.getAvatar()).isEqualTo("userAvtar");
  }

  private void assertRepo(io.harness.ngtriggers.beans.scm.Repository repository) {
    assertThat(repository).isNotNull();
    assertThat(repository.getName()).isEqualTo("myRepo");
    assertThat(repository.getNamespace()).isEqualTo("myNamespace");
    assertThat(repository.getLink()).isEqualTo("myLink");
    assertThat(repository.getBranch()).isEqualTo("master");
    assertThat(repository.getHttpURL()).isEqualTo("https://myRepo");
    assertThat(repository.getSshURL()).isEqualTo("git@myRepo");
  }
}
