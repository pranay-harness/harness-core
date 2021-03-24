package io.harness.ngtriggers.utils;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.beans.CommitDetails;
import io.harness.beans.HeaderConfig;
import io.harness.beans.PushWebhookEvent;
import io.harness.beans.Repository;
import io.harness.beans.WebhookBaseAttributes;
import io.harness.beans.WebhookGitUser;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.aws.codecommit.AwsCodeCommitApiTaskResponse;
import io.harness.delegate.beans.aws.codecommit.AwsCodeCommitDataObtainmentTaskResult;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.entity.metadata.GitMetadata;
import io.harness.ngtriggers.beans.entity.metadata.NGTriggerMetadata;
import io.harness.ngtriggers.beans.entity.metadata.WebhookMetadata;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.product.ci.scm.proto.Signature;
import io.harness.product.ci.scm.proto.User;
import io.harness.rule.Owner;

import com.google.protobuf.Timestamp;
import java.io.IOException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class AwsCodeCommitDataObtainerTest extends CategoryTest {
  @Mock private WebhookEventPayloadParser webhookEventPayloadParser;
  @Spy @InjectMocks AwsCodeCommitDataObtainer awsCodeCommitDataObtainer;
  private static final List<TriggerDetails> triggerDetailsList;
  private static final Repository repository =
      Repository.builder().httpURL("https://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test").build();

  private static final WebhookGitUser webhookGitUser =
      WebhookGitUser.builder()
          .gitId("arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
          .build();

  private static final PushWebhookEvent pushWebhookEvent =
      PushWebhookEvent.builder()
          .branchName("main")
          .commitDetailsList(singletonList(
              CommitDetails.builder()
                  .commitId("f70e8226cac251f6116315984b6e9ed7098ce586")
                  .ownerId("arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                  .timeStamp(1616318557000L)
                  .build()))
          .repository(repository)
          .baseAttributes(
              WebhookBaseAttributes.builder()
                  .ref("refs/heads/main")
                  .target("main")
                  .authorLogin(
                      "arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                  .sender("arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                  .build())
          .build();

  private static final TriggerWebhookEvent triggerWebhookEvent =
      TriggerWebhookEvent.builder()
          .payload("{\n"
              + "  \"Type\" : \"Notification\",\n"
              + "  \"MessageId\" : \"2d8bef2e-9233-543b-90c0-b7ff80aec631\",\n"
              + "  \"TopicArn\" : \"arn:aws:sns:eu-central-1:44864EXAMPLE:aws_cc_push_trigger\",\n"
              + "  \"Subject\" : \"UPDATE: AWS CodeCommit eu-central-1 push: test\",\n"
              + "  \"Message\" : \"{\\\"Records\\\":[{\\\"awsRegion\\\":\\\"eu-central-1\\\",\\\"codecommit\\\":{\\\"references\\\":[{\\\"commit\\\":\\\"f70e8226cac251f6116315984b6e9ed7098ce586\\\",\\\"ref\\\":\\\"refs/heads/main\\\"}]},\\\"customData\\\":null,\\\"eventId\\\":\\\"6363f269-6070-4999-9b55-52c4e40d74b0\\\",\\\"eventName\\\":\\\"ReferenceChanges\\\",\\\"eventPartNumber\\\":1,\\\"eventSource\\\":\\\"aws:codecommit\\\",\\\"eventSourceARN\\\":\\\"arn:aws:codecommit:eu-central-1:44864EXAMPLE:test\\\",\\\"eventTime\\\":\\\"2021-03-21T09:22:37.156+0000\\\",\\\"eventTotalParts\\\":1,\\\"eventTriggerConfigId\\\":\\\"e15f4b05-bec2-47f8-9505-c10df79f4c42\\\",\\\"eventTriggerName\\\":\\\"push\\\",\\\"eventVersion\\\":\\\"1.0\\\",\\\"userIdentityARN\\\":\\\"arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029\\\"}]}\",\n"
              + "  \"Timestamp\" : \"2021-03-21T09:22:37.198Z\",\n"
              + "  \"SignatureVersion\" : \"1\",\n"
              + "  \"Signature\" : \"DnmP7IBvzHPEqm8054phY6aQztpvTktsBNTLqOeX1j48t65vFKxpyrhFvOKDh6vispOFZO+RgJjTvG7vq9LEEMtns56sLoNcZUYtyydRQC3KQdQjzFhPx+M/oZX81WxWc3gIUVNVteIF4izObmi9NUrU1ioiW+D/IBFVVbzW7Yw3TJT8SeF6verQdImDBuQ7izPVEPzz1RsZH/9DN8iQg5F5rMnQwADZzjNLLQ+hpbh0Addy40x89TMzjSYXYLSOS24hxxpnftWP9LTrZVlekwMz+NqFMhX6mzKWXI80mYnRqaCwDdy7ZaEskJvW+9gByl05MH++b6ZJGdIhmgyVMw==\",\n"
              + "  \"SigningCertURL\" : \"https://sns.eu-central-1.amazonaws.com/SimpleNotificationService-010a507c1833636cd94bdb98bd93083a.pem\",\n"
              + "  \"UnsubscribeURL\" : \"https://sns.eu-central-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-central-1:44864EXAMPLE:aws_cc_push_trigger:05b595dd-06fb-4002-b409-80a7a7bbfda6\"\n"
              + "}")
          .headers(
              asList(HeaderConfig.builder().key("X-Amz-Sns-Message-Type").values(singletonList("Notification")).build(),
                  HeaderConfig.builder()
                      .key("X-Amz-Sns-Message-Id")
                      .values(singletonList("2d8bef2e-9233-543b-90c0-b7ff80aec631"))
                      .build(),
                  HeaderConfig.builder()
                      .key("X-Amz-Sns-Topic-Arn")
                      .values(singletonList("arn:aws:sns:eu-central-1:44864EXAMPLE:aws_cc_push_trigger"))
                      .build()))
          .accountId("acc")
          .orgIdentifier("org")
          .projectIdentifier("proj")
          .sourceRepoType("AWS_CODECOMMIT")
          .build();

  private static final Commit commit =
      Commit.newBuilder()
          .setSha("f70e8226cac251f6116315984b6e9ed7098ce586")
          .setAuthor(
              Signature.newBuilder()
                  .setDate(Timestamp.newBuilder().setSeconds(1616318557).build())
                  .setLogin("arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                  .build())
          .setCommitter(
              Signature.newBuilder()
                  .setDate(Timestamp.newBuilder().setSeconds(1616318557).build())
                  .setLogin("arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                  .build())
          .build();

  private static final ParseWebhookResponse parseWebhookResponse =
      ParseWebhookResponse.newBuilder()
          .setPush(PushHook.newBuilder()
                       .setRef("refs/heads/main")
                       .setRepo(io.harness.product.ci.scm.proto.Repository.newBuilder()
                                    .setId("arn:aws:codecommit:eu-central-1:44864EXAMPLE:test")
                                    .setName("test")
                                    .setBranch("main")
                                    .setPrivate(true)
                                    .setCreated(Timestamp.newBuilder().setSeconds(62135596800L).build())
                                    .setUpdated(Timestamp.newBuilder().setSeconds(62135596800L).build())

                                    .build())
                       .addCommits(commit)
                       .setCommit(commit)
                       .build())
          .build();

  private static final WebhookPayloadData webhookPayloadData = WebhookPayloadData.builder()
                                                                   .webhookGitUser(webhookGitUser)
                                                                   .parseWebhookResponse(parseWebhookResponse)
                                                                   .webhookEvent(pushWebhookEvent)
                                                                   .repository(repository)
                                                                   .originalEvent(triggerWebhookEvent)
                                                                   .build();

  static {
    TriggerDetails details1 =
        TriggerDetails.builder()
            .ngTriggerEntity(
                NGTriggerEntity.builder()
                    .accountId("acc")
                    .orgIdentifier("org")
                    .projectIdentifier("proj")
                    .metadata(NGTriggerMetadata.builder()
                                  .webhook(WebhookMetadata.builder()
                                               .type("AWS_CODECOMMIT")
                                               .git(GitMetadata.builder().connectorIdentifier("account.con1").build())
                                               .build())
                                  .build())
                    .build())
            .build();

    TriggerDetails details2 =
        TriggerDetails.builder()
            .ngTriggerEntity(
                NGTriggerEntity.builder()
                    .accountId("acc")
                    .orgIdentifier("org")
                    .projectIdentifier("proj")
                    .metadata(
                        NGTriggerMetadata.builder()
                            .webhook(
                                WebhookMetadata.builder()
                                    .type("AWS_CODECOMMIT")
                                    .git(
                                        GitMetadata.builder().repoName("repo2").connectorIdentifier("org.con1").build())
                                    .build())
                            .build())
                    .build())
            .build();

    triggerDetailsList = asList(details1, details2);
  }

  private static final AwsCodeCommitApiTaskResponse awsCodeCommitApiTaskResponse =
      AwsCodeCommitApiTaskResponse.builder()
          .awsCodecommitApiResult(
              AwsCodeCommitDataObtainmentTaskResult.builder()
                  .commitDetailsList(singletonList(CommitDetails.builder()
                                                       .commitId("f70e8226cac251f6116315984b6e9ed7098ce586")
                                                       .message("Initial commit")
                                                       .ownerName("Firstname Lastname")
                                                       .ownerEmail("firstname.lastname@gitmail.io")
                                                       .timeStamp(1616317729)
                                                       .build()))
                  .repository(Repository.builder()
                                  .id("37489034-ca56-411e-85c9-58fa8dffaab0")
                                  .name("test")
                                  .branch("main")
                                  .httpURL("https://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test")
                                  .sshURL("ssh://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test")
                                  .build())
                  .webhookGitUser(
                      WebhookGitUser.builder()
                          .name("user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                          .gitId(
                              "arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                          .build())
                  .build())
          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
          .build();

  public static final ParseWebhookResponse obtainedParseWebhookResponse =
      ParseWebhookResponse.newBuilder()
          .setPush(
              PushHook.newBuilder()
                  .setRef("refs/heads/main")
                  .setRepo(io.harness.product.ci.scm.proto.Repository.newBuilder()
                               .setId("37489034-ca56-411e-85c9-58fa8dffaab0")
                               .setName("test")
                               .setBranch("main")
                               .setPrivate(true)
                               .setClone("https://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test")
                               .setCloneSsh("ssh://git-codecommit.eu-central-1.amazonaws.com/v1/repos/test")
                               .setCreated(Timestamp.newBuilder().setSeconds(62135596800L).build())
                               .setUpdated(Timestamp.newBuilder().setSeconds(62135596800L).build())
                               .build())
                  .setSender(
                      User.newBuilder()
                          .setLogin(
                              "arn:aws:iam::44864EXAMPLE:user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                          .setName("user/vault-okta-username.lastname@gitmail.io-a1616318518-9029")
                          .build())
                  .addCommits(Commit.newBuilder()
                                  .setSha("f70e8226cac251f6116315984b6e9ed7098ce586")
                                  .setMessage("Initial commit")
                                  .setAuthor(Signature.newBuilder()
                                                 .setName("Firstname Lastname")
                                                 .setEmail("firstname.lastname@gitmail.io")
                                                 .build())
                                  .setCommitter(Signature.newBuilder()
                                                    .setDate(Timestamp.newBuilder().setSeconds(1616317).build())
                                                    .build())
                                  .build())
                  .setCommit(commit)
                  .build())
          .build();
  private static final WebhookPayloadData expectedWebhookPayloadData =
      WebhookPayloadData.builder().parseWebhookResponse(obtainedParseWebhookResponse).build();

  @Before
  public void setUp() throws IOException {
    initMocks(this);
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldAcquireAwsCodeCommitData() {
    FilterRequestData filterRequestData = FilterRequestData.builder()
                                              .details(triggerDetailsList)
                                              .projectFqn("acc/org/proj")
                                              .webhookPayloadData(webhookPayloadData)
                                              .build();

    doReturn(awsCodeCommitApiTaskResponse)
        .when(awsCodeCommitDataObtainer)
        .buildAndExecuteAwsCodeCommitDelegateTask(eq(webhookPayloadData), any(), any());

    doReturn(expectedWebhookPayloadData)
        .when(webhookEventPayloadParser)
        .convertWebhookResponse(eq(obtainedParseWebhookResponse), any());

    awsCodeCommitDataObtainer.acquireProviderData(filterRequestData);

    assertThat(filterRequestData).isNotNull();
    assertThat(filterRequestData.getWebhookPayloadData()).isNotNull();
    assertThat(filterRequestData.getWebhookPayloadData().getParseWebhookResponse())
        .isEqualTo(obtainedParseWebhookResponse);
  }
}