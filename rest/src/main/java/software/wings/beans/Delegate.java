package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.List;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Entity(value = "delegates", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class Delegate extends Base {
  public static final String ACCOUNT_ID_KEY = "accountId";
  public static final String HOST_NAME_KEY = "hostName";
  public static final String VERSION_KEY = "version";

  @NotEmpty private String accountId;
  private Status status = Status.ENABLED;
  private String description;
  private boolean connected;
  private String ip;
  private String hostName;
  private String delegateName;
  private String delegateProfileId;
  private long lastHeartBeat;
  private String version;
  @Deprecated private List<String> supportedTaskTypes;

  @Transient private List<DelegateTask> currentlyExecutingDelegateTasks;

  private List<DelegateScope> includeScopes;
  private List<DelegateScope> excludeScopes;
  private List<String> tags;

  public enum Status { ENABLED, DISABLED }

  public static final class Builder {
    private String accountId;
    private Status status = Status.ENABLED;
    private boolean connected;
    private String ip;
    private String description;
    private String hostName;
    private String delegateName;
    private String delegateProfileId;
    private long lastHeartBeat;
    private String version;
    private List<DelegateScope> includeScopes;
    private List<DelegateScope> excludeScopes;
    private List<DelegateTask> currentlyExecutingDelegateTasks;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private List<String> tags;

    private Builder() {}

    public static Builder aDelegate() {
      return new Builder();
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withStatus(Status status) {
      this.status = status;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withConnected(boolean connected) {
      this.connected = connected;
      return this;
    }

    public Builder withIp(String ip) {
      this.ip = ip;
      return this;
    }

    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    public Builder withDelegateName(String delegateName) {
      this.delegateName = delegateName;
      return this;
    }

    public Builder withDelegateProfileId(String delegateProfileId) {
      this.delegateProfileId = delegateProfileId;
      return this;
    }

    public Builder withLastHeartBeat(long lastHeartBeat) {
      this.lastHeartBeat = lastHeartBeat;
      return this;
    }

    public Builder withVersion(String version) {
      this.version = version;
      return this;
    }

    public Builder withIncludeScopes(List<DelegateScope> includeScopes) {
      this.includeScopes = includeScopes;
      return this;
    }

    public Builder withExcludeScopes(List<DelegateScope> excludeScopes) {
      this.excludeScopes = excludeScopes;
      return this;
    }

    public Builder withCurrentlyExecutingDelegateTasks(List<DelegateTask> currentlyExecutingDelegateTasks) {
      this.currentlyExecutingDelegateTasks = currentlyExecutingDelegateTasks;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withTags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    public Builder but() {
      return aDelegate()
          .withAccountId(accountId)
          .withStatus(status)
          .withDescription(description)
          .withConnected(connected)
          .withIp(ip)
          .withHostName(hostName)
          .withDelegateName(delegateName)
          .withDelegateProfileId(delegateProfileId)
          .withLastHeartBeat(lastHeartBeat)
          .withVersion(version)
          .withIncludeScopes(includeScopes)
          .withExcludeScopes(excludeScopes)
          .withTags(tags)
          .withCurrentlyExecutingDelegateTasks(currentlyExecutingDelegateTasks)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public Delegate build() {
      Delegate delegate = new Delegate();
      delegate.setAccountId(accountId);
      delegate.setStatus(status);
      delegate.setDescription(description);
      delegate.setConnected(connected);
      delegate.setIp(ip);
      delegate.setHostName(hostName);
      delegate.setDelegateName(delegateName);
      delegate.setDelegateProfileId(delegateProfileId);
      delegate.setLastHeartBeat(lastHeartBeat);
      delegate.setVersion(version);
      delegate.setIncludeScopes(includeScopes);
      delegate.setExcludeScopes(excludeScopes);
      delegate.setTags(tags);
      delegate.setCurrentlyExecutingDelegateTasks(currentlyExecutingDelegateTasks);
      delegate.setUuid(uuid);
      delegate.setAppId(appId);
      delegate.setCreatedBy(createdBy);
      delegate.setCreatedAt(createdAt);
      delegate.setLastUpdatedBy(lastUpdatedBy);
      delegate.setLastUpdatedAt(lastUpdatedAt);
      return delegate;
    }
  }
}
