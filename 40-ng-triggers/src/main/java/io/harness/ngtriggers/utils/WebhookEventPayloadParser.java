package io.harness.ngtriggers.utils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.grpc.StatusRuntimeException;
import io.harness.exception.InvalidRequestException;
import io.harness.ngtriggers.beans.config.HeaderConfig;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.scm.Repository;
import io.harness.ngtriggers.beans.scm.*;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData.WebhookPayloadDataBuilder;
import io.harness.product.ci.scm.proto.*;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Singleton
public class WebhookEventPayloadParser {
  private static final String X_GIT_HUB_EVENT = "X-GitHub-Event";
  private static final String X_GIT_LAB_EVENT = "X-Gitlab-Event";
  private static final String X_BIT_BUCKET_EVENT = "X-Event-Key";

  @Inject private SCMGrpc.SCMBlockingStub scmBlockingStub;

  public WebhookPayloadData parseEvent(TriggerWebhookEvent triggerWebhookEvent) {
    ParseWebhookResponse parseWebhookResponse = invokeScmService(triggerWebhookEvent);

    log.info(parseWebhookResponse.toString());
    return convertWebhookResponse(parseWebhookResponse, triggerWebhookEvent);
  }

  @VisibleForTesting
  ParseWebhookResponse invokeScmService(TriggerWebhookEvent triggerWebhookEvent) {
    Set<String> headerKeys = triggerWebhookEvent.getHeaders().stream().map(HeaderConfig::getKey).collect(toSet());

    GitProvider gitProvider = obtainWebhookSource(headerKeys);
    Header.Builder header = Header.newBuilder();
    triggerWebhookEvent.getHeaders().forEach(headerConfig
        -> header.addFields(
            Header.Pair.newBuilder().setKey(headerConfig.getKey()).addAllValues(headerConfig.getValues()).build()));

    ParseWebhookResponse parseWebhookResponse;
    try {
      parseWebhookResponse = scmBlockingStub.parseWebhook(ParseWebhookRequest.newBuilder()
                                                              .setBody(triggerWebhookEvent.getPayload())
                                                              .setProvider(gitProvider)
                                                              .setHeader(header.build())
                                                              .build());
      log.info("This is a parseWebhookResponse:");
    } catch (StatusRuntimeException e) {
      log.error("Failed to parse webhook payload {}", triggerWebhookEvent.getPayload());
      throw e;
    }
    return parseWebhookResponse;
  }

  public WebhookPayloadData convertWebhookResponse(
      ParseWebhookResponse parseWebhookResponse, TriggerWebhookEvent triggerWebhookEvent) {
    WebhookPayloadDataBuilder webhookPayloadDataBuilder;
    if (parseWebhookResponse.hasPr()) {
      PullRequestHook prHook = parseWebhookResponse.getPr();
      webhookPayloadDataBuilder = convertPullRequestHook(prHook);
    } else if (parseWebhookResponse.hasPush()) {
      PushHook pushHook = parseWebhookResponse.getPush();
      if (pushHook.getRef().startsWith("refs/tags/")) {
        throw new InvalidRequestException("Tag event not supported", USER);
      }
      webhookPayloadDataBuilder = convertPushHook(pushHook);
    } else {
      log.error("Unknown webhook event");
      throw new InvalidRequestException("Unknown webhook event", USER);
    }

    webhookPayloadDataBuilder.originalEvent(triggerWebhookEvent);
    return webhookPayloadDataBuilder.build();
  }

  private WebhookPayloadDataBuilder convertPullRequestHook(PullRequestHook prHook) {
    WebhookGitUser webhookGitUser = convertUser(prHook.getSender());
    PRWebhookEvent prWebhookEvent = convertPRWebhookEvent(prHook);

    return WebhookPayloadData.builder()
        .webhookGitUser(webhookGitUser)
        .repository(prWebhookEvent.getRepository())
        .webhookEvent(prWebhookEvent);
  }

  WebhookPayloadDataBuilder convertPushHook(PushHook pushHook) {
    WebhookGitUser webhookGitUser = convertUser(pushHook.getSender());
    BranchWebhookEvent webhookEvent = convertPushWebhookEvent(pushHook);

    return WebhookPayloadData.builder()
        .webhookGitUser(webhookGitUser)
        .repository(webhookEvent.getRepository())
        .webhookEvent(webhookEvent);
  }

  private BranchWebhookEvent convertPushWebhookEvent(PushHook pushHook) {
    // TODO Add required push event details here with commit
    List<CommitDetails> commitDetailsList = new ArrayList<>();
    pushHook.getCommitsList().forEach(commit -> commitDetailsList.add(convertCommit(commit)));

    return BranchWebhookEvent.builder()
        .branchName(pushHook.getRepo().getBranch())
        .link(pushHook.getRepo().getLink())
        .commitDetailsList(commitDetailsList)
        .repository(convertRepository(pushHook.getRepo()))
        .baseAttributes(convertPushHookBaseAttributes(pushHook))
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
        .timeStamp(commit.getCommitter().getDate().getSeconds() * 1000)
        .ownerEmail(commit.getAuthor().getEmail())
        .ownerId(commit.getAuthor().getLogin())
        .ownerName(commit.getAuthor().getName())
        .build();
  }

  private Repository convertRepository(io.harness.product.ci.scm.proto.Repository repo) {
    return Repository.builder()
        .name(repo.getName())
        .namespace(repo.getNamespace())
        .slug(repo.getNamespace() + "/" + repo.getName())
        .link(repo.getLink())
        .branch(repo.getBranch())
        .isPrivate(repo.getPrivate())
        .httpURL(repo.getClone())
        .sshURL(repo.getCloneSsh())
        .build();
  }

  private PRWebhookEvent convertPRWebhookEvent(PullRequestHook prHook) {
    // TODO Add commit details
    PullRequest pr = prHook.getPr();
    return PRWebhookEvent.builder()
        .sourceBranch(pr.getSource())
        .targetBranch(pr.getTarget())
        .pullRequestLink(pr.getLink())
        .pullRequestBody(pr.getBody())
        .pullRequestId(pr.getNumber())
        .title(pr.getTitle())
        .closed(pr.getClosed())
        .merged(pr.getMerged())
        .repository(convertRepository(prHook.getRepo()))
        .baseAttributes(convertPrHookBaseAttributes(prHook))
        .build();
  }

  private WebhookBaseAttributes convertPrHookBaseAttributes(PullRequestHook prHook) {
    PullRequest pr = prHook.getPr();
    User author = prHook.getPr().getAuthor();
    String message = pr.getBody();
    if (message.equals("")) {
      message = pr.getTitle();
    }
    return WebhookBaseAttributes.builder()
        .link(pr.getLink())
        .message(message)
        .before(pr.getBase().getSha())
        .after(pr.getSha())
        .ref(pr.getRef())
        .source(pr.getSource())
        .target(pr.getTarget())
        .authorLogin(author.getLogin())
        .authorName(author.getName())
        .authorEmail(author.getEmail())
        .authorAvatar(author.getAvatar())
        .sender(prHook.getSender().getLogin())
        .action(prHook.getAction().toString().toLowerCase())
        .build();
  }

  private WebhookBaseAttributes convertPushHookBaseAttributes(PushHook pushHook) {
    String trimmedRef = pushHook.getRef().replaceFirst("^refs/heads/", "");
    Signature author = pushHook.getCommit().getAuthor();
    return WebhookBaseAttributes.builder()
        .link(pushHook.getCommit().getLink())
        .message(pushHook.getCommit().getMessage())
        .before(pushHook.getBefore())
        .after(pushHook.getAfter())
        .ref(pushHook.getRef())
        .source(trimmedRef)
        .target(trimmedRef)
        .authorLogin(author.getLogin())
        .authorName(author.getName())
        .authorEmail(author.getEmail())
        .authorAvatar(author.getAvatar())
        .sender(pushHook.getSender().getLogin())
        .build();
  }

  private GitProvider obtainWebhookSource(Set<String> headerKeys) {
    if (isEmpty(headerKeys)) {
      throw new InvalidRequestException("Failed to resolve Webhook Source. Reason: HttpHeaders are empty.");
    }

    if (headerKeys.contains(X_GIT_HUB_EVENT)) {
      return GitProvider.GITHUB;
    } else if (headerKeys.contains(X_GIT_LAB_EVENT)) {
      return GitProvider.GITLAB;
    } else if (headerKeys.contains(X_BIT_BUCKET_EVENT)) {
      return GitProvider.BITBUCKET;
    }
    throw new InvalidRequestException("Unable to resolve the Webhook Source. "
            + "One of " + X_GIT_HUB_EVENT + ", " + X_BIT_BUCKET_EVENT + ", " + X_GIT_LAB_EVENT
            + " must be present in Headers",
        USER);
  }
}
