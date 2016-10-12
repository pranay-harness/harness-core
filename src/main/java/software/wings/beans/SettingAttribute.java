package software.wings.beans;

import com.google.common.base.Objects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

/**
 * Created by anubhaw on 5/16/16.
 */
@Entity(value = "settingAttributes")
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("envId"), @Field("name") }, options = @IndexOptions(unique = true)))
public class SettingAttribute extends Base {
  @NotEmpty private String envId = GLOBAL_ENV_ID;
  private String name;
  private SettingValue value;

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets value.
   *
   * @return the value
   */
  public SettingValue getValue() {
    return value;
  }

  /**
   * Sets value.
   *
   * @param value the value
   */
  public void setValue(SettingValue value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hashCode(envId, name, value);
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
    final SettingAttribute other = (SettingAttribute) obj;
    return Objects.equal(this.envId, other.envId) && Objects.equal(this.name, other.name)
        && Objects.equal(this.value, other.value);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String envId = GLOBAL_ENV_ID;
    private String name;
    private SettingValue value;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * A setting attribute builder.
     *
     * @return the builder
     */
    public static Builder aSettingAttribute() {
      return new Builder();
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With value builder.
     *
     * @param value the value
     * @return the builder
     */
    public Builder withValue(SettingValue value) {
      this.value = value;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aSettingAttribute()
          .withEnvId(envId)
          .withName(name)
          .withValue(value)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build setting attribute.
     *
     * @return the setting attribute
     */
    public SettingAttribute build() {
      SettingAttribute settingAttribute = new SettingAttribute();
      settingAttribute.setEnvId(envId);
      settingAttribute.setName(name);
      settingAttribute.setValue(value);
      settingAttribute.setUuid(uuid);
      settingAttribute.setAppId(appId);
      settingAttribute.setCreatedBy(createdBy);
      settingAttribute.setCreatedAt(createdAt);
      settingAttribute.setLastUpdatedBy(lastUpdatedBy);
      settingAttribute.setLastUpdatedAt(lastUpdatedAt);
      return settingAttribute;
    }
  }
}
