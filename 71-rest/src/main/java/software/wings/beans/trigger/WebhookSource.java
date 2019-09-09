package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum WebhookSource {
  GITHUB,
  GITLAB,
  BITBUCKET;

  public interface WebhookEvent {}

  public enum GitHubEventType implements WebhookEvent {
    PULL_REQUEST("Pull Request", "pull_request", WebhookEventType.PULL_REQUEST),
    PUSH("Push", "push", WebhookEventType.PUSH),
    PING("Ping", "ping", WebhookEventType.PING),
    DELETE("Delete", "delete", WebhookEventType.DELETE),
    ANY("Any", "any", WebhookEventType.ANY),
    OTHER("Other", "other", WebhookEventType.OTHER),
    PULL_REQUEST_CLOSED("Closed", "closed", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_EDITED("Edited", "edited", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_OPENED("Opened", "opened", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_REOPENED("Reopened", "reopened", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_ASSIGNED("Assigned", "assigned", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_UNASSIGNED("Unassigned", "unassigned", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_LABELED("Labeled", "labeled", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_UNLABELED("Unlabeled", "unlabeled", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_SYNCHRONIZED("Synchronized", "synchronize", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_REVIEW_REQUESTED("Review Requested", "review_requested", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_REVIEW_REQUESTED_REMOVED(
        "Review Request Removed", "review_request_removed", WebhookEventType.PULL_REQUEST);

    @Getter private String displayName;
    @Getter private String value;
    @Getter private WebhookEventType eventType;

    GitHubEventType(String displayName, String value, WebhookEventType eventType) {
      this.displayName = displayName;
      this.value = value;
      this.eventType = eventType;
      GHEventHolder.map.put(value, this);
    }

    public static class GHEventHolder { @Getter static Map<String, GitHubEventType> map = new HashMap<>(); }

    public static GitHubEventType find(String val) {
      return GitHubEventType.GHEventHolder.map.get(val);
    }
  }

  public enum BitBucketEventType implements WebhookEvent {
    PING("Ping", "ping", WebhookEventType.PING),
    DIAGNOSTICS_PING("Diagnostics Ping", "diagnostics:ping", WebhookEventType.PING),
    ALL("All", "all", WebhookEventType.ANY),

    ANY("Any", "any", WebhookEventType.ANY),
    OTHER("Other", "other", WebhookEventType.OTHER),
    FORK("Fork", "repo:fork", WebhookEventType.PUSH),
    UPDATED("Updated", "repo:updated", WebhookEventType.PUSH),
    COMMIT_COMMENT_CREATED("Commit Comment Created", "repo:commit_comment_created", WebhookEventType.PUSH),
    BUILD_STATUS_CREATED("Build Status Created", "repo:commit_status_created", WebhookEventType.PUSH),
    BUILD_STATUS_UPDATED("Build Status Updated", "repo:commit_status_updated", WebhookEventType.PUSH),
    PUSH("Push", "repo:push", WebhookEventType.PUSH),
    REFS_CHANGED("Refs Changed", "repo:refs_changed", WebhookEventType.PUSH),

    ISSUE_CREATED("Issue Created", "issue:created", WebhookEventType.ISSUE),
    ISSUE_UPDATED("Issue Updated", "issue:updated", WebhookEventType.ISSUE),
    ISSUE_COMMENT_CREATED("Issue Comment Created", "issue:comment_created", WebhookEventType.ISSUE),

    PULL_REQUEST_CREATED("Pull Request Created", "pullrequest:created", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_UPDATED("Pull Request Updated", "pullrequest:updated", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_APPROVED("Pull Request Approved", "pullrequest:approved", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_APPROVAL_REMOVED(
        "Pull Request Approval Removed", "pullrequest:unapproved", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_MERGED("Pull Request Merged", "pullrequest:fulfilled", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_DECLINED("Pull Request Declined", "pullrequest:rejected", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_COMMENT_CREATED(
        "Pull Request Comment Created", "pullrequest:comment_created", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_COMMENT_UPDATED(
        "Pull Request Comment Updated", "pullrequest:comment_updated", WebhookEventType.PULL_REQUEST),
    PULL_REQUEST_COMMENT_DELETED(
        "Pull Request Comment Deleted", "pullrequest:comment_deleted", WebhookEventType.PULL_REQUEST);

    @Getter private String displayName;
    @Getter private String value;
    @Getter private WebhookEventType eventType;

    BitBucketEventType(String displayName, String value, WebhookEventType eventType) {
      this.displayName = displayName;
      this.value = value;
      this.eventType = eventType;
      BitBucketEventHolder.map.put(value, this);
    }

    public static class BitBucketEventHolder { @Getter static Map<String, BitBucketEventType> map = new HashMap<>(); }

    public static BitBucketEventType find(String val) {
      return BitBucketEventType.BitBucketEventHolder.map.get(val);
    }

    public static boolean containsAllEvent(List<BitBucketEventType> bitBucketEventType) {
      if (bitBucketEventType.contains(ALL) || bitBucketEventType.contains(ANY)) {
        return true;
      } else {
        return false;
      }
    }
  }

  public enum GitLabEventType implements WebhookEvent {
    PULL_REQUEST("Pull Request", "Merge Request Hook", WebhookEventType.PULL_REQUEST),
    PUSH("Push", "Push Hook", WebhookEventType.PUSH),
    PING("Ping", "ping", WebhookEventType.PING),
    ANY("Any", "any", WebhookEventType.ANY),
    OTHER("Other", "other", WebhookEventType.OTHER);

    @Getter private String displayName;
    @Getter private String value;
    @Getter private WebhookEventType eventType;

    GitLabEventType(String displayName, String eventKeyValue, WebhookEventType eventType) {
      this.displayName = displayName;
      this.value = eventKeyValue;
      this.eventType = eventType;
      GitLabEventHolder.map.put(eventKeyValue, this);
    }

    public static class GitLabEventHolder { @Getter static Map<String, GitLabEventType> map = new HashMap<>(); }

    public static GitLabEventType find(String eventKeyValue) {
      return GitLabEventType.GitLabEventHolder.map.get(eventKeyValue);
    }
  }

  @Value
  @Builder
  public static class WebhookSubEventInfo {
    String displayValue;
    String enumName;
  }

  @Value
  @Builder
  public static class WebhookEventInfo {
    String displayValue;
    String enumName;
    List<WebhookSubEventInfo> subEvents;
  }
}
