package software.wings.yaml;

import static software.wings.yaml.YamlVersion.Builder.aYamlVersion;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

import java.util.Objects;

/**
 * Created by bsollish on 8/30/17
 */
@Entity(value = "yamlVersion", noClassnameStored = true)
@Indexes(@Index(fields = { @Field("entityId") }, options = @IndexOptions(name = "yamlVersionIdx")))
public class YamlVersion extends Base implements YamlHistory {
  private String yamlVersionId;
  private int version;
  private Type type;
  private String entityId;
  private long inEffectStart;
  private long inEffectEnd;
  private String yaml;

  @SchemaIgnore private String accountId;

  public YamlVersion() {}

  public String getYamlVersionId() {
    return getUuid();
  }

  public void setYamlVersionId(String yamlVersionId) {
    this.yamlVersionId = getUuid();
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
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

  public long getInEffectStart() {
    return inEffectStart;
  }

  public void setInEffectStart(long inEffectStart) {
    this.inEffectStart = inEffectStart;
  }

  public long getInEffectEnd() {
    return inEffectEnd;
  }

  public void setInEffectEnd(long inEffectEnd) {
    this.inEffectEnd = inEffectEnd;
  }

  public String getYaml() {
    return yaml;
  }

  public void setYaml(String yaml) {
    this.yaml = yaml;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, entityId, yaml);
  }

  @SuppressFBWarnings("EQ_OVERRIDING_EQUALS_NOT_SYMMETRIC") // TODO
  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof YamlVersion)) {
      return false;
    }
    YamlVersion yv = (YamlVersion) o;
    return Objects.equals(type, yv.type) && Objects.equals(entityId, yv.entityId) && Objects.equals(yaml, yv.yaml);
  }

  @Override
  public String toString() {
    MoreObjects.toStringHelper(this);
    return MoreObjects.toStringHelper(this)
        .add("yamlVersionId", yamlVersionId)
        .add("version", version)
        .add("type", type)
        .add("entityId", entityId)
        .add("inEffectStart", inEffectStart)
        .add("inEffectEnd", inEffectEnd)
        .add("yaml", yaml)
        .add("accountId", accountId)
        .toString();
  }

  public enum Type {
    SETUP,
    APP,
    SERVICE,
    SERVICE_COMMAND,
    CONFIG_FILE,
    ENVIRONMENT,
    CONFIG_FILE_OVERRIDE,
    SETTING,
    WORKFLOW,
    PIPELINE,
    PROVISIONER,
    TRIGGER,
    ARTIFACT_STREAM,
    INFRA_MAPPING,
    DEPLOYMENT_SPEC,
    APPLICATION_DEFAULTS,
    ACCOUNT_DEFAULTS,
    NOTIFICATION_GROUP
  }

  @SuppressFBWarnings("CN_IMPLEMENTS_CLONE_BUT_NOT_CLONEABLE") // TODO
  public YamlVersion clone() {
    return aYamlVersion()
        .withUuid(getYamlVersionId())
        .withVersion(getVersion())
        .withType(getType())
        .withEntityId(getEntityId())
        .withInEffectStart(getInEffectStart())
        .withInEffectEnd(getInEffectEnd())
        .withYaml(getYaml())
        .withAccountId(getAccountId())
        .build();
  }

  public static final class Builder {
    private String uuid;
    private int version;
    private Type type;
    private String entityId;
    private long inEffectStart;
    private long inEffectEnd;
    private String yaml;

    private String accountId;

    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static YamlVersion.Builder aYamlVersion() {
      return new YamlVersion.Builder();
    }

    public YamlVersion.Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public YamlVersion.Builder withVersion(int version) {
      this.version = version;
      return this;
    }

    public YamlVersion.Builder withType(Type type) {
      this.type = type;
      return this;
    }

    public YamlVersion.Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public YamlVersion.Builder withInEffectStart(long inEffectStart) {
      this.inEffectStart = inEffectStart;
      return this;
    }

    public YamlVersion.Builder withInEffectEnd(long inEffectEnd) {
      this.inEffectEnd = inEffectEnd;
      return this;
    }

    public YamlVersion.Builder withYaml(String yaml) {
      this.yaml = yaml;
      return this;
    }

    public YamlVersion.Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    @SuppressFBWarnings("URF_UNREAD_FIELD") // TODO
    public YamlVersion.Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    @SuppressFBWarnings("URF_UNREAD_FIELD") // TODO
    public YamlVersion.Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    @SuppressFBWarnings("URF_UNREAD_FIELD") // TODO
    public YamlVersion.Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    @SuppressFBWarnings("URF_UNREAD_FIELD") // TODO
    public YamlVersion.Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public YamlVersion.Builder but() {
      return aYamlVersion()
          .withUuid(uuid)
          .withVersion(version)
          .withType(type)
          .withEntityId(entityId)
          .withInEffectStart(inEffectStart)
          .withInEffectEnd(inEffectEnd)
          .withYaml(yaml)
          .withAccountId(accountId);
    }

    public YamlVersion build() {
      YamlVersion yamlVersion = new YamlVersion();
      yamlVersion.setYamlVersionId(uuid);
      yamlVersion.setVersion(version);
      yamlVersion.setType(type);
      yamlVersion.setEntityId(entityId);
      yamlVersion.setInEffectStart(inEffectStart);
      yamlVersion.setInEffectEnd(inEffectEnd);
      yamlVersion.setYaml(yaml);
      yamlVersion.setAccountId(accountId);
      return yamlVersion;
    }
  }
}
