package software.wings.yaml.gitSync;

import static software.wings.yaml.gitSync.YamlGitSync.Builder.aYamlGitSync;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Indexes(@Index(
    fields = { @Field("entityId") }, options = @IndexOptions(name = "yamlGitSyncIdx", unique = true, dropDups = true)))
public class YamlGitSync extends Base {
  @JsonIgnore private Type type;
  private String entityId;
  private String directoryPath;

  private boolean enabled;
  private String url;
  private String rootPath;
  private String sshKey;
  private String passphrase;
  private SyncMode syncMode;

  @SchemaIgnore private String accountId;

  public YamlGitSync() {}

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

  public String getDirectoryPath() {
    return directoryPath;
  }

  public void setDirectoryPath(String directoryPath) {
    this.directoryPath = directoryPath;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
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
    this.sshKey = sshKey;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
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
  public String getAppId() {
    return appId;
  }

  @Override
  public void setAppId(String appId) {
    this.appId = appId;
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
    return Objects.equals(type, ygs.type) && Objects.equals(entityId, ygs.entityId)
        && Objects.equals(directoryPath, ygs.directoryPath);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("uuid", getUuid())
        .add("type", type)
        .add("entityId", entityId)
        .add("enabled", enabled)
        .add("directoryPath", directoryPath)
        .add("url", url)
        .add("rootPath", rootPath)
        .add("syncMode", syncMode)
        .add("accountId", accountId)
        .add("appId", appId)
        .toString();
  }

  public enum SyncMode { GIT_TO_HARNESS, HARNESS_TO_GIT, BOTH, NONE }

  public enum Type { SETUP, APP, SERVICE, SERVICE_COMMAND, ENVIRONMENT, SETTING, WORKFLOW, PIPELINE, TRIGGER, FOLDER }

  public YamlGitSync clone() {
    return aYamlGitSync()
        .withUuid(getUuid())
        .withType(getType())
        .withEntityId(getEntityId())
        .withDirectoryPath(getDirectoryPath())
        .withEnabled(isEnabled())
        .withUrl(getUrl())
        .withRootPath(getRootPath())
        .withSshKey(getSshKey())
        .withPassphrase(getPassphrase())
        .withSyncMode(getSyncMode())
        .withAccountId(getAccountId())
        .withAppId(getAppId())
        .build();
  }

  public static Type convertRestNameToType(String restName) {
    switch (restName) {
      case "setup":
        return Type.SETUP;
      case "applications":
        return Type.APP;
      case "services":
        return Type.SERVICE;
      case "service-commands":
        return Type.SERVICE_COMMAND;
      case "environments":
        return Type.ENVIRONMENT;
      case "settings":
        return Type.SETTING;
      case "workflows":
        return Type.WORKFLOW;
      case "pipelines":
        return Type.PIPELINE;
      case "triggers":
        return Type.TRIGGER;
      case "folders":
        return Type.FOLDER;
      default:
        // do nothing
    }

    return null;
  }

  public static final class Builder {
    private String uuid;
    private Type type;
    private String entityId;
    private String directoryPath;
    private boolean enabled;
    private String url;
    private String rootPath;
    private String sshKey;
    private String passphrase;
    private SyncMode syncMode;

    private String accountId;
    private String appId;

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

    public YamlGitSync.Builder withDirectoryPath(String directoryPath) {
      this.directoryPath = directoryPath;
      return this;
    }

    public YamlGitSync.Builder withEnabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    public YamlGitSync.Builder withUrl(String url) {
      this.url = url;
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

    public YamlGitSync.Builder withPassphrase(String passphrase) {
      this.passphrase = passphrase;
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

    public YamlGitSync.Builder withAppId(String appId) {
      this.appId = appId;
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
          .withDirectoryPath(directoryPath)
          .withEnabled(enabled)
          .withUrl(url)
          .withRootPath(rootPath)
          .withSshKey(sshKey)
          .withPassphrase(passphrase)
          .withSyncMode(syncMode)
          .withAccountId(accountId)
          .withAppId(appId);
    }

    public YamlGitSync build() {
      YamlGitSync yamlGitSync = new YamlGitSync();
      yamlGitSync.setUuid(uuid);
      yamlGitSync.setType(type);
      yamlGitSync.setEntityId(entityId);
      yamlGitSync.setDirectoryPath(directoryPath);
      yamlGitSync.setEnabled(enabled);

      yamlGitSync.setUrl(url);
      yamlGitSync.setRootPath(rootPath);
      yamlGitSync.setSshKey(sshKey);
      yamlGitSync.setPassphrase(passphrase);
      yamlGitSync.setSyncMode(syncMode);

      yamlGitSync.setAccountId(accountId);
      yamlGitSync.setAppId(appId);
      return yamlGitSync;
    }
  }
}