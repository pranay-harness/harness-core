package software.wings.yaml.gitSync;

import static software.wings.yaml.gitSync.YamlGitSync.Builder.aYamlGitSync;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Objects;

/**
 * Created by bsollish
 */
@Entity(value = "yamlGitSync", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("entityId") }, options = @IndexOptions(name = "yamlGitSyncIdx")))
public class YamlGitSync extends Base {
  private String yamlGitSyncId;
  private Type type;
  private String entityId;

  private boolean enabled;
  private String URL;
  private String rootPath;
  private String sshKey;
  private SyncMode syncMode;

  @SchemaIgnore private String accountId;

  public YamlGitSync() {}

  public String getYamlGitSyncId() {
    return yamlGitSyncId;
  }

  public void setYamlGitSyncId(String yamlGitSyncId) {
    this.yamlGitSyncId = yamlGitSyncId;
  }

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getURL() {
    return URL;
  }

  public void setURL(String URL) {
    this.URL = URL;
  }

  public String getRootPath() {
    return rootPath;
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public String getSshKey() {
    return sshKey;
  }

  public void setSshKey(String sshKey) {
    sshKey = sshKey;
  }

  public SyncMode getSyncMode() {
    return syncMode;
  }

  public void setSyncMode(SyncMode syncMode) {
    this.syncMode = syncMode;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, entityId);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;
    if (!(o instanceof YamlGitSync)) {
      return false;
    }
    YamlGitSync ygs = (YamlGitSync) o;
    return Objects.equals(type, ygs.type) && Objects.equals(entityId, ygs.entityId);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("yamlGitSyncId", yamlGitSyncId)
        .add("type", type)
        .add("entityId", entityId)
        .add("enabled", enabled)
        .add("URL", URL)
        .add("rootPath", rootPath)
        .add("syncMode", syncMode)
        .add("accountId", accountId)
        .toString();
  }

  public enum SyncMode { GIT_TO_HARNESS, HARNESS_TO_GIT, BOTH }

  public enum Type { SETUP, APP, SERVICE, SERVICE_COMMAND, ENVIRONMENT, SETTING, WORKFLOW, PIPELINE, TRIGGER }

  public YamlGitSync clone() {
    return aYamlGitSync()
        .withUuid(getYamlGitSyncId())
        .withType(getType())
        .withEntityId(getEntityId())
        .withEnabled(isEnabled())
        .withURL(getURL())
        .withRootPath(getRootPath())
        .withSshKey(getSshKey())
        .withSyncMode(getSyncMode())
        .withAccountId(getAccountId())
        .build();
  }

  public static final class Builder {
    private String uuid;
    private Type type;
    private String entityId;
    private boolean enabled;
    private String URL;
    private String rootPath;
    private String sshKey;
    private SyncMode syncMode;

    private String accountId;

    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static YamlGitSync.Builder aYamlGitSync() {
      return new YamlGitSync.Builder();
    }

    public YamlGitSync.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public YamlGitSync.Builder withType(Type type) {
      this.type = type;
      return this;
    }

    public YamlGitSync.Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public YamlGitSync.Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public YamlGitSync.Builder withURL(String URL) {
      this.URL = URL;
      return this;
    }

    public YamlGitSync.Builder withRootPath(String rootPath) {
      this.rootPath = rootPath;
      return this;
    }

    public YamlGitSync.Builder withSshKey(String sshKey) {
      this.sshKey = sshKey;
      return this;
    }

    public YamlGitSync.Builder withSyncMode(SyncMode syncMode) {
      this.syncMode = syncMode;
      return this;
    }

    public YamlGitSync.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public YamlGitSync.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public YamlGitSync.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public YamlGitSync.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public YamlGitSync.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public YamlGitSync.Builder but() {
      return aYamlGitSync()
          .withUuid(uuid)
          .withType(type)
          .withEntityId(entityId)
          .withEnabled(enabled)
          .withURL(URL)
          .withRootPath(rootPath)
          .withSshKey(sshKey)
          .withSyncMode(syncMode)
          .withAccountId(accountId);
    }

    public YamlGitSync build() {
      YamlGitSync yamlGitSync = new YamlGitSync();
      yamlGitSync.setYamlGitSyncId(uuid);
      yamlGitSync.setType(type);
      yamlGitSync.setEntityId(entityId);
      yamlGitSync.setEnabled(enabled);

      yamlGitSync.setURL(URL);
      yamlGitSync.setRootPath(rootPath);
      yamlGitSync.setSshKey(sshKey);
      yamlGitSync.setSyncMode(syncMode);

      yamlGitSync.setAccountId(accountId);
      return yamlGitSync;
    }
  }
}