package software.wings.beans.trigger;

import lombok.Getter;

import java.util.HashMap;
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
    PULL_REQUEST("Pull Request", "pull_request"),
    PUSH("Push", "repo:push"),
    PING("Ping", "ping");

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
