package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.product.ci.scm.proto.BitbucketCloudWebhookEvent;
import io.harness.product.ci.scm.proto.BitbucketServerWebhookEvent;
import io.harness.product.ci.scm.proto.GithubWebhookEvent;
import io.harness.product.ci.scm.proto.GitlabWebhookEvent;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;

@OwnedBy(HarnessTeam.DX)
public enum HookEventType {
  @JsonProperty("TRIGGER_EVENTS")
  TRIGGER_EVENTS(
      Arrays.asList(GithubWebhookEvent.GITHUB_CREATE, GithubWebhookEvent.GITHUB_PUSH, GithubWebhookEvent.GITHUB_DELETE,
          GithubWebhookEvent.GITHUB_DEPLOYMENT, GithubWebhookEvent.GITHUB_PULL_REQUEST,
          GithubWebhookEvent.GITHUB_PULL_REQUEST_REVIEW, GithubWebhookEvent.GITHUB_ISSUE_COMMENT),
      Arrays.asList(GitlabWebhookEvent.GITLAB_COMMENT, GitlabWebhookEvent.GITLAB_ISSUES,
          GitlabWebhookEvent.GITLAB_MERGE, GitlabWebhookEvent.GITLAB_PUSH, GitlabWebhookEvent.GITLAB_TAG),
      Arrays.asList(BitbucketCloudWebhookEvent.BITBUCKET_CLOUD_ISSUE,
          BitbucketCloudWebhookEvent.BITBUCKET_CLOUD_PULL_REQUEST, BitbucketCloudWebhookEvent.BITBUCKET_CLOUD_PUSH,
          BitbucketCloudWebhookEvent.BITBUCKET_CLOUD_ISSUE_COMMENT,
          BitbucketCloudWebhookEvent.BITBUCKET_CLOUD_PULL_REQUEST_COMMENT),
      Arrays.asList(BitbucketServerWebhookEvent.BITBUCKET_SERVER_PR,
          BitbucketServerWebhookEvent.BITBUCKET_SERVER_BRANCH_PUSH_TAG,
          BitbucketServerWebhookEvent.BITBUCKET_SERVER_PR_COMMENT));

  public List<GithubWebhookEvent> githubWebhookEvents;
  public List<GitlabWebhookEvent> gitlabWebhookEvents;
  public List<BitbucketCloudWebhookEvent> bitbucketCloudWebhookEvents;
  public List<BitbucketServerWebhookEvent> bitbucketServerWebhookEvents;

  HookEventType(List<GithubWebhookEvent> githubWebhookEvents, List<GitlabWebhookEvent> gitlabWebhookEvents,
      List<BitbucketCloudWebhookEvent> bitbucketCloudWebhookEvents,
      List<BitbucketServerWebhookEvent> bitbucketServerWebhookEvents) {
    this.githubWebhookEvents = githubWebhookEvents;
    this.gitlabWebhookEvents = gitlabWebhookEvents;
    this.bitbucketCloudWebhookEvents = bitbucketCloudWebhookEvents;
    this.bitbucketServerWebhookEvents = bitbucketServerWebhookEvents;
  }
}
