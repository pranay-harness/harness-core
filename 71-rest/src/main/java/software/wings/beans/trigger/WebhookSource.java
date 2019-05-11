package software.wings.beans.trigger;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum WebhookSource {
  GITHUB,
  GITLAB,
  BITBUCKET;

  public enum EventType {}

  public enum GitHubEventType {
    PULL_REQUEST("Pull Request", "pull_request"),
    PUSH("Push", "push"),
    PING("Ping", "ping"),
    DELETE("Delete", "delete");

    @Getter private String displayName;
    @Getter private String value;

    GitHubEventType(String displayName, String value) {
      this.displayName = displayName;
      this.value = value;
      GHEventHolder.map.put(value, this);
    }

    private static class GHEventHolder { static Map<String, GitHubEventType> map = new HashMap<>(); }

    public static GitHubEventType find(String val) {
      return GitHubEventType.GHEventHolder.map.get(val);
    }
  }

  public enum BitBucketEventType {
    PING("Ping", "ping"),
    ALL("All", "all"),

    FORK("Fork", "repo:fork"),
    UPDATED("Updated", "repo:updated"),
    COMMIT_COMMENT_CREATED("Commit Comment Created", "repo:commit_comment_created"),
    COMMIT_STATUS_CREATED("Commit Status Created", "repo:commit_status_created"),
    COMMIT_STATUS_UPDATED("Commit Status Updated", "repo:commit_status_updated"),
    PUSH("Push", "repo:push"),

    ISSUE_CREATED("Issue Created", "issue:created"),
    ISSUE_UPDATED("Issue Updated", "issue:updated"),
    ISSUE_COMMENT_CREATED("Issue Comment Created", "issue:comment_created"),

    PULL_REQUEST_CREATED("Pull Request Created", "pullrequest:created"),
    PULL_REQUEST_UPDATED("Pull Request Updated", "pullrequest:updated"),
    PULL_REQUEST_APPROVED("Pull Request Approved", "pullrequest:approved"),
    PULL_REQUEST_APPROVAL_REMOVED("Pull Request Approval Removed", "pullrequest:unapproved"),
    PULL_REQUEST_MERGED("Pull Request Merged", "pullrequest:fulfilled"),
    PULL_REQUEST_DECLINED("Pull Request Declined", "pullrequest:rejected"),
    PULL_REQUEST_COMMENT_CREATED("Pull Request Comment Created", "pullrequest:comment_created"),
    PULL_REQUEST_COMMENT_UPDATED("Pull Request Comment Updated", "pullrequest:comment_updated"),
    PULL_REQUEST_COMMENT_DELETED("Pull Request Comment Deleted", "pullrequest:comment_deleted");

    @Getter private String displayName;
    @Getter private String value;

    BitBucketEventType(String displayName, String value) {
      this.displayName = displayName;
      this.value = value;
      BitBucketEventHolder.map.put(value, this);
    }

    private static class BitBucketEventHolder { static Map<String, BitBucketEventType> map = new HashMap<>(); }

    public static BitBucketEventType find(String val) {
      return BitBucketEventType.BitBucketEventHolder.map.get(val);
    }

    public static boolean containsAllEvent(List<BitBucketEventType> bitBucketEventType) {
      if (bitBucketEventType.contains(ALL)) {
        return true;
      } else {
        return false;
      }
    }
  }

  public enum GitLabEventType {
    PULL_REQUEST("Pull Request", "Merge Request Hook"),
    PUSH("Push", "Push Hook"),
    PING("Ping", "ping");

    @Getter private String displayName;
    @Getter private String value;

    GitLabEventType(String displayName, String eventKeyValue) {
      this.displayName = displayName;
      this.value = eventKeyValue;
      GitLabEventHolder.map.put(eventKeyValue, this);
    }

    private static class GitLabEventHolder { static Map<String, GitLabEventType> map = new HashMap<>(); }

    public static GitLabEventType find(String eventKeyValue) {
      return GitLabEventType.GitLabEventHolder.map.get(eventKeyValue);
    }
  }
}
