package io.harness.ngtriggers.beans.source.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.EnumSet;
import java.util.Set;

public enum WebhookEvent {
  @JsonProperty("Pull Request") PULL_REQUEST,
  @JsonProperty("Push") PUSH,
  @JsonProperty("Issue Comment") ISSUE_COMMENT,
  @JsonProperty("Delete") DELETE,
  @JsonProperty("Merge Request") MERGE_REQUEST,
  @JsonProperty("Repository") REPOSITORY,
  @JsonProperty("Branch") BRANCH,
  @JsonProperty("Tag") TAG;

  public static final Set<WebhookEvent> githubEvents = EnumSet.of(PULL_REQUEST, PUSH, ISSUE_COMMENT);
  public static final Set<WebhookEvent> gitlabEvents = EnumSet.of(PUSH, MERGE_REQUEST);
  public static final Set<WebhookEvent> bitbucketEvents = EnumSet.of(PULL_REQUEST, PUSH);
  public static final Set<WebhookEvent> awsCodeCommitEvents = EnumSet.of(PUSH, BRANCH, TAG);
}
