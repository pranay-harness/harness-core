package software.wings.beans;

import static java.util.Arrays.stream;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ARTIFACTORY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AMAZON_S3;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.BAMBOO;
import static software.wings.settings.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.DIRECT;
import static software.wings.settings.SettingValue.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingValue.SettingVariableTypes.ECR;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELB;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCR;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.JENKINS;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEXUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.settings.SettingValue.SettingVariableTypes.SLACK;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPLUNK;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELK;
import static software.wings.settings.SettingValue.SettingVariableTypes.STRING;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.List;
import java.util.Objects;
import javax.validation.Valid;

/**
 * Created by anubhaw on 5/16/16.
 */
@Entity(value = "settingAttributes")
@Indexes(
    @Index(fields = { @Field("accountId")
                      , @Field("appId"), @Field("envId"), @Field("name"), @Field("value.type") },
        options = @IndexOptions(unique = true)))
public class SettingAttribute extends Base {
  @NotEmpty private String envId = GLOBAL_ENV_ID;
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  @Valid private SettingValue value;
  private Category category = Category.SETTING;
  private List<String> appIds;

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

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Getter for property 'category'.
   *
   * @return Value for property 'category'.
   */
  public Category getCategory() {
    return category;
  }

  /**
   * Setter for property 'category'.
   *
   * @param category Value to set for property 'category'.
   */
  public void setCategory(Category category) {
    this.category = category;
  }

  /**
   * Getter for property 'appIds'.
   *
   * @return Value for property 'appIds'.
   */
  public List<String> getAppIds() {
    return appIds;
  }

  /**
   * Setter for property 'appIds'.
   *
   * @param appIds Value to set for property 'appIds'.
   */
  public void setAppIds(List<String> appIds) {
    this.appIds = appIds;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(envId, accountId, name, value, category, appIds);
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
    return Objects.equals(this.envId, other.envId) && Objects.equals(this.accountId, other.accountId)
        && Objects.equals(this.name, other.name) && Objects.equals(this.value, other.value)
        && Objects.equals(this.category, other.category) && Objects.equals(this.appIds, other.appIds);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("envId", envId)
        .add("accountId", accountId)
        .add("name", name)
        .add("value", value)
        .add("category", category)
        .add("appIds", appIds)
        .toString();
  }

  public enum Category {
    CLOUD_PROVIDER(Lists.newArrayList(PHYSICAL_DATA_CENTER, AWS, GCP, DIRECT)),

    CONNECTOR(Lists.newArrayList(
        SMTP, JENKINS, BAMBOO, SPLUNK, ELK, APP_DYNAMICS, ELB, SLACK, DOCKER, ECR, GCR, NEXUS, ARTIFACTORY, AMAZON_S3)),

    SETTING(Lists.newArrayList(HOST_CONNECTION_ATTRIBUTES, BASTION_HOST_CONNECTION_ATTRIBUTES, STRING));

    private List<SettingVariableTypes> settingVariableTypes;

    Category(List<SettingVariableTypes> settingVariableTypes) {
      this.settingVariableTypes = settingVariableTypes;
    }

    public static Category getCategory(SettingVariableTypes settingVariableType) {
      return stream(Category.values())
          .filter(category -> category.settingVariableTypes.contains(settingVariableType))
          .findFirst()
          .orElse(null);
    }
  }

  public static final class Builder {
    private String envId = GLOBAL_ENV_ID;
    private String accountId;
    private String name;
    private SettingValue value;
    private Category category = Category.SETTING;
    private List<String> appIds;
    private String uuid;
    private String appId = GLOBAL_APP_ID;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    public static Builder aSettingAttribute() {
      return new Builder();
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withValue(SettingValue value) {
      this.value = value;
      return this;
    }

    public Builder withCategory(Category category) {
      this.category = category;
      return this;
    }

    public Builder withAppIds(List<String> appIds) {
      this.appIds = appIds;
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

    public Builder but() {
      return aSettingAttribute()
          .withEnvId(envId)
          .withAccountId(accountId)
          .withName(name)
          .withValue(value)
          .withCategory(category)
          .withAppIds(appIds)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    public SettingAttribute build() {
      SettingAttribute settingAttribute = new SettingAttribute();
      settingAttribute.setEnvId(envId);
      settingAttribute.setAccountId(accountId);
      settingAttribute.setName(name);
      settingAttribute.setValue(value);
      settingAttribute.setCategory(category);
      settingAttribute.setAppIds(appIds);
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
