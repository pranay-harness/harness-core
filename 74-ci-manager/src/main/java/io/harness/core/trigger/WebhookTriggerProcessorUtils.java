package io.harness.core.trigger;

import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.StatusRuntimeException;
import io.harness.beans.execution.BranchWebhookEvent;
import io.harness.beans.execution.CommitDetails;
import io.harness.beans.execution.PRWebhookEvent;
import io.harness.beans.execution.WebhookEvent;
import io.harness.beans.execution.WebhookExecutionSource;
import io.harness.beans.execution.WebhookGitUser;
import io.harness.exception.InvalidRequestException;
import io.harness.product.ci.scm.proto.Commit;
import io.harness.product.ci.scm.proto.GitProvider;
import io.harness.product.ci.scm.proto.Header;
import io.harness.product.ci.scm.proto.ParseWebhookRequest;
import io.harness.product.ci.scm.proto.ParseWebhookResponse;
import io.harness.product.ci.scm.proto.PullRequest;
import io.harness.product.ci.scm.proto.PullRequestHook;
import io.harness.product.ci.scm.proto.PushHook;
import io.harness.product.ci.scm.proto.SCMGrpc;
import io.harness.product.ci.scm.proto.User;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.HttpHeaders;

@Singleton
@Slf4j
public class WebhookTriggerProcessorUtils {
  private static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  private static final String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  private static final String X_BIT_BUCKET_EVENT = "X-Event-Key";

  @Inject private SCMGrpc.SCMBlockingStub scmBlockingStub;

  public WebhookExecutionSource fetchWebhookExecutionSource(String eventPayload, HttpHeaders httpHeaders) {
    Header.Builder header = Header.newBuilder();
    httpHeaders.getRequestHeaders().forEach(
        (k, v) -> { header.addFields(Header.Pair.newBuilder().setKey(k).addAllValues(v).build()); });

    GitProvider gitProvider = obtainWebhookSource(httpHeaders);
    ParseWebhookResponse parseWebhookResponse;
    try {
      parseWebhookResponse = scmBlockingStub.parseWebhook(ParseWebhookRequest.newBuilder()
                                                              .setBody(eventPayload)
                                                              .setProvider(gitProvider)
                                                              .setHeader(header.build())
                                                              .build());
    } catch (StatusRuntimeException e) {
      logger.error("Failed to parse webhook payload", eventPayload);
      throw e;
    }

    logger.info(parseWebhookResponse.toString());
    return convertWebhookResponse(parseWebhookResponse);
  }

  private WebhookExecutionSource convertWebhookResponse(ParseWebhookResponse parseWebhookResponse) {
    if (parseWebhookResponse.hasPr()) {
      PullRequestHook prHook = parseWebhookResponse.getPr();
      return convertPullRequestHook(prHook);
    } else if (parseWebhookResponse.hasPush()) {
      PushHook pushHook = parseWebhookResponse.getPush();
      return converPushHook(pushHook);
    } else {
      logger.error("Unknown webhook event");
      throw new InvalidRequestException("Unknown webhook event", USER);
    }
  }

  private WebhookExecutionSource convertPullRequestHook(PullRequestHook prHook) {
    WebhookGitUser webhookGitUser = convertUser(prHook.getSender());
    WebhookEvent webhookEvent = convertPRWebhookEvent(prHook.getPr());

    return WebhookExecutionSource.builder().user(webhookGitUser).webhookEvent(webhookEvent).build();
  }

  private WebhookExecutionSource converPushHook(PushHook pushHook) {
    WebhookGitUser webhookGitUser = convertUser(pushHook.getSender());
    WebhookEvent webhookEvent = convertPushWebhookEvent(pushHook);

    return WebhookExecutionSource.builder().user(webhookGitUser).webhookEvent(webhookEvent).build();
  }

  private WebhookEvent convertPushWebhookEvent(PushHook pushHook) {
    // TODO Add required push event details here with commit
    List<CommitDetails> commitDetailsList = new ArrayList<>();
    pushHook.getCommitsList().forEach(commit -> commitDetailsList.add(convertCommit(commit)));

    return BranchWebhookEvent.builder()
        .branchName(pushHook.getRepo().getBranch())
        .link(pushHook.getRepo().getLink())
        .commitDetailsList(commitDetailsList)
        .build();
  }

  private WebhookEvent convertPRWebhookEvent(PullRequest pullRequest) {
    // TODO Add commit details
    return PRWebhookEvent.builder()
        .sourceBranch(pullRequest.getSource())
        .targetBranch(pullRequest.getTarget())
        .pullRequestLink(pullRequest.getLink())
        .pullRequestBody(pullRequest.getBody())
        .pullRequestId(pullRequest.getNumber())
        .title(pullRequest.getTitle())
        .closed(pullRequest.getClosed())
        .merged(pullRequest.getMerged())
        .build();
  }

  private WebhookGitUser convertUser(User user) {
    return WebhookGitUser.builder()
        .avatar(user.getAvatar())
        .email(user.getEmail())
        .gitId(user.getLogin())
        .name(user.getName())
        .build();
  }

  private CommitDetails convertCommit(Commit commit) {
    return CommitDetails.builder()
        .commitId(commit.getSha())
        .message(commit.getMessage())
        .link(commit.getLink())
        .ownerEmail(commit.getAuthor().getEmail())
        .ownerId(commit.getAuthor().getLogin())
        .ownerName(commit.getAuthor().getName())
        .build();
  }

  private GitProvider obtainWebhookSource(HttpHeaders httpHeaders) {
    if (httpHeaders == null) {
      throw new InvalidRequestException("Failed to resolve Webhook Source. Reason: HttpHeaders are empty.");
    }

    if (httpHeaders.getHeaderString(X_GIT_HUB_EVENT) != null) {
      return GitProvider.GITHUB;
    } else if (httpHeaders.getHeaderString(X_GIT_LAB_EVENT) != null) {
      return GitProvider.GITLAB;
    } else if (httpHeaders.getHeaderString(X_BIT_BUCKET_EVENT) != null) {
      return GitProvider.BITBUCKET;
    }
    throw new InvalidRequestException("Unable to resolve the Webhook Source. "
            + "One of " + X_GIT_HUB_EVENT + ", " + X_BIT_BUCKET_EVENT + ", " + X_GIT_LAB_EVENT
            + " must be present in Headers",
        USER);
  }
}
