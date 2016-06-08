package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/16/16.
 */
@Entity(value = "settingAttributes")
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class SettingAttribute extends Base {
  private String name;
  private SettingValue value;

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

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, value);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
   */
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
    return Objects.equals(this.name, other.name) && Objects.equals(this.value, other.value);
  }

  /**
   * The Class SettingAttributeBuilder.
   */
  public static final class SettingAttributeBuilder {
    private String name;
    private SettingValue value;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private SettingAttributeBuilder() {}

    /**
     * A setting attribute.
     *
     * @return the setting attribute builder
     */
    public static SettingAttributeBuilder aSettingAttribute() {
      return new SettingAttributeBuilder();
    }

    /**
     * With name.
     *
     * @param name the name
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With value.
     *
     * @param value the value
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withValue(SettingValue value) {
      this.value = value;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the setting attribute builder
     */
    public SettingAttributeBuilder but() {
      return aSettingAttribute()
          .withName(name)
          .withValue(value)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Builds the.
     *
     * @return the setting attribute
     */
    public SettingAttribute build() {
      SettingAttribute settingAttribute = new SettingAttribute();
      settingAttribute.setName(name);
      settingAttribute.setValue(value);
      settingAttribute.setUuid(uuid);
      settingAttribute.setAppId(appId);
      settingAttribute.setCreatedBy(createdBy);
      settingAttribute.setCreatedAt(createdAt);
      settingAttribute.setLastUpdatedBy(lastUpdatedBy);
      settingAttribute.setLastUpdatedAt(lastUpdatedAt);
      settingAttribute.setActive(active);
      return settingAttribute;
    }
  }
}
