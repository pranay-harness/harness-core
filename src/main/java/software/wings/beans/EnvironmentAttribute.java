package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

/**
 * Created by anubhaw on 5/16/16.
 */

@Entity(value = "environmentVariables")
@Indexes(@Index(fields = { @Field("envId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class EnvironmentAttribute extends Base {
  private String envId;
  private String name;
  private EnvironmentValue value;

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public EnvironmentValue getValue() {
    return value;
  }

  public void setValue(EnvironmentValue value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, name, value);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final EnvironmentAttribute other = (EnvironmentAttribute) obj;
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.name, other.name)
        && Objects.equals(this.value, other.value);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("envId", envId).add("name", name).add("value", value).toString();
  }

  public static final class EnvironmentVariableBuilder {
    private String envId;
    private String key;
    private EnvironmentValue value;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private EnvironmentVariableBuilder() {}

    public static EnvironmentVariableBuilder anEnvironmentVariable() {
      return new EnvironmentVariableBuilder();
    }

    public EnvironmentVariableBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public EnvironmentVariableBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    public EnvironmentVariableBuilder withValue(EnvironmentValue value) {
      this.value = value;
      return this;
    }

    public EnvironmentVariableBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public EnvironmentVariableBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public EnvironmentVariableBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public EnvironmentVariableBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public EnvironmentVariableBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public EnvironmentVariableBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public EnvironmentVariableBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public EnvironmentVariableBuilder but() {
      return anEnvironmentVariable()
          .withEnvId(envId)
          .withKey(key)
          .withValue(value)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public EnvironmentAttribute build() {
      EnvironmentAttribute environmentAttribute = new EnvironmentAttribute();
      environmentAttribute.setEnvId(envId);
      environmentAttribute.setName(key);
      environmentAttribute.setValue(value);
      environmentAttribute.setUuid(uuid);
      environmentAttribute.setAppId(appId);
      environmentAttribute.setCreatedBy(createdBy);
      environmentAttribute.setCreatedAt(createdAt);
      environmentAttribute.setLastUpdatedBy(lastUpdatedBy);
      environmentAttribute.setLastUpdatedAt(lastUpdatedAt);
      environmentAttribute.setActive(active);
      return environmentAttribute;
    }
  }
}
