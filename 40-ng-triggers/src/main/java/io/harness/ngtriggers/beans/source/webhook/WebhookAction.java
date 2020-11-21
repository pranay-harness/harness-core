package io.harness.ngtriggers.beans.source.webhook;

import io.harness.exception.InvalidRequestException;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum WebhookAction {
  @JsonProperty("assigned") ASSIGNED,
  @JsonProperty("closed") CLOSED,
  @JsonProperty("edited") EDITED,
  @JsonProperty("labeled") LABELED,
  @JsonProperty("opened") OPENED,
  @JsonProperty("review requested") REVIEW_REQUESTED,
  @JsonProperty("review requested removed") REVIEW_REQUESTED_REMOVED,
  @JsonProperty("reopened") REOPENED,
  @JsonProperty("synchronized") SYNCHRONIZED,
  @JsonProperty("unassigned") UNASSIGNED,
  @JsonProperty("unlabeled") UNLABELED,
  @JsonProperty("published") PUBLISHED,
  @JsonProperty("created") CREATED,
  @JsonProperty("deleted") DELETED,
  @JsonProperty("pre-released") PRE_RELEASED,
  @JsonProperty("released") RELEASED,
  @JsonProperty("unpublished") UNPUBLISHED,
  @JsonProperty("all") ALL,
  @JsonProperty("pull request created") PULL_REQUEST_CREATED,
  @JsonProperty("pull request updated") PULL_REQUEST_UPDATED,
  @JsonProperty("pull request approved") PULL_REQUEST_APPROVED,
  @JsonProperty("pull request approval removed") PULL_REQUEST_APPROVAL_REMOVED,
  @JsonProperty("pull request merged") PULL_REQUEST_MERGED,
  @JsonProperty("pull request declined") PULL_REQUEST_DECLINED,
  @JsonProperty("pull request comment created") PULL_REQUEST_COMMENT_CREATED,
  @JsonProperty("pull request comment updated") PULL_REQUEST_COMMENT_UPDATED,
  @JsonProperty("pull request comment deleted") PULL_REQUEST_COMMENT_DELETED,
  @JsonProperty("push") PUSH,
  @JsonProperty("fork") FORK,
  @JsonProperty("updated") UPDATED,
  @JsonProperty("commit comment created") COMMIT_COMMENT_CREATED,
  @JsonProperty("build status created") BUILD_STATUS_CREATED,
  @JsonProperty("build status updated") BUILD_STATUS_UPDATED,
  @JsonProperty("reference changed") REFERENCE_CHANGED,
  @JsonProperty("issue created") ISSUE_CREATED,
  @JsonProperty("issue updated") ISSUE_UPDATED,
  @JsonProperty("issue comment created") ISSUE_COMMENT_CREATED;

  public static Set<WebhookAction> getGithubActionForEvent(WebhookEvent event) {
    if (!WebhookEvent.githubEvents.contains(event)) {
      throw new InvalidRequestException("Event " + event.name() + "not a github event");
    }

    switch (event) {
      case PULL_REQUEST:
        return EnumSet.of(ASSIGNED, CLOSED, EDITED, LABELED, OPENED, REVIEW_REQUESTED, REVIEW_REQUESTED_REMOVED,
            REOPENED, SYNCHRONIZED, UNASSIGNED, UNLABELED);
      case PACKAGE:
        return EnumSet.of(PUBLISHED);
      case RELEASE:
        return EnumSet.of(CREATED, DELETED, EDITED, PRE_RELEASED, PUBLISHED, RELEASED, UNPUBLISHED);
      case PUSH:
      case DELETE:
        return Collections.emptySet();
      default:
        throw new InvalidRequestException("Event " + event.name() + "not a github event");
    }
  }

  public static Set<WebhookAction> getBitbucketActionForEvent(WebhookEvent event) {
    if (!WebhookEvent.bitbucketEvents.contains(event)) {
      throw new InvalidRequestException("Event " + event.name() + "not a bitbucket event");
    }
    switch (event) {
      case PULL_REQUEST:
        return EnumSet.of(ALL, PULL_REQUEST_CREATED, PULL_REQUEST_UPDATED, PULL_REQUEST_APPROVED,
            PULL_REQUEST_APPROVAL_REMOVED, PULL_REQUEST_MERGED, PULL_REQUEST_DECLINED, PULL_REQUEST_COMMENT_CREATED,
            PULL_REQUEST_COMMENT_UPDATED, PULL_REQUEST_COMMENT_DELETED);
      case REPOSITORY:
        return EnumSet.of(ALL, PUSH, FORK, UPDATED, COMMIT_COMMENT_CREATED, BUILD_STATUS_CREATED, BUILD_STATUS_UPDATED,
            REFERENCE_CHANGED);
      case ISSUE:
        return EnumSet.of(ISSUE_CREATED, ISSUE_UPDATED, ISSUE_COMMENT_CREATED);
      default:
        throw new InvalidRequestException("Event " + event.name() + "not a bitbucket event");
    }
  }
}
