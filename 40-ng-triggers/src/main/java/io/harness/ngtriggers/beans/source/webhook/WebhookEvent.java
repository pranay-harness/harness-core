package io.harness.ngtriggers.beans.source.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.EnumSet;
import java.util.Set;

public enum WebhookEvent {
  @JsonProperty("Pull Request") PULL_REQUEST,
  @JsonProperty("Push") PUSH,
  @JsonProperty("Issue") ISSUE,
  @JsonProperty("Package") PACKAGE,
  @JsonProperty("Release") RELEASE,
  @JsonProperty("Delete") DELETE,
  @JsonProperty("Merge Request") MERGE_REQUEST,
  @JsonProperty("Repository") REPOSITORY;

  public static final Set<WebhookEvent> githubEvents = EnumSet.of(PULL_REQUEST, PACKAGE, RELEASE, PUSH, DELETE);
  public static final Set<WebhookEvent> gitlabEvents = EnumSet.of(PUSH, MERGE_REQUEST);
  public static final Set<WebhookEvent> bitbucketEvents = EnumSet.of(PULL_REQUEST, REPOSITORY, ISSUE);
}
