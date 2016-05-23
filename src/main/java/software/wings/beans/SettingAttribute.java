package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;

import java.util.Objects;

/**
 * Created by anubhaw on 5/16/16.
 */

@Entity(value = "settingAttributes")
@Indexes(@Index(fields = { @Field("appId")
                           , @Field("name") }, options = @IndexOptions(unique = true)))
public class SettingAttribute extends Base {
  private String name;
  private SettingValue value;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SettingValue getValue() {
    return value;
  }

  public void setValue(SettingValue value) {
    this.value = value;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, value);
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
    return Objects.equals(this.name, other.name) && Objects.equals(this.value, other.value);
  }

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

    public static SettingAttributeBuilder aSettingAttribute() {
      return new SettingAttributeBuilder();
    }

    public SettingAttributeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public SettingAttributeBuilder withValue(SettingValue value) {
      this.value = value;
      return this;
    }

    public SettingAttributeBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public SettingAttributeBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public SettingAttributeBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public SettingAttributeBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public SettingAttributeBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public SettingAttributeBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public SettingAttributeBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

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
