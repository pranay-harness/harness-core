package software.wings.beans;

import static java.util.Arrays.stream;
import static software.wings.settings.SettingValue.SettingVariableTypes.AMAZON_S3;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ARTIFACTORY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.BAMBOO;
import static software.wings.settings.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.DIRECT;
import static software.wings.settings.SettingValue.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingValue.SettingVariableTypes.ECR;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELB;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELK;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCR;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.JENKINS;
import static software.wings.settings.SettingValue.SettingVariableTypes.LOGZ;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEW_RELIC;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEXUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.settings.SettingValue.SettingVariableTypes.SLACK;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPLUNK;
import static software.wings.settings.SettingValue.SettingVariableTypes.STRING;
import static software.wings.settings.SettingValue.SettingVariableTypes.SUMO;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.BaseYaml;

import java.util.List;
import javax.validation.Valid;

/**
 * Created by anubhaw on 5/16/16.
 */
@Entity(value = "settingAttributes")
@Indexes(
    @Index(fields = { @Field("accountId")
                      , @Field("appId"), @Field("envId"), @Field("name"), @Field("value.type") },
        options = @IndexOptions(unique = true)))
@Data
public class SettingAttribute extends Base {
  @NotEmpty private String envId = GLOBAL_ENV_ID;
  @NotEmpty private String accountId;
  @NotEmpty private String name;
  @Valid private SettingValue value;
  private Category category = Category.SETTING;
  private List<String> appIds;

  @SchemaIgnore @Transient private transient EncryptionType encryptionType;

  @SchemaIgnore @Transient private transient String encryptedBy;

  public enum Category {
    CLOUD_PROVIDER(Lists.newArrayList(PHYSICAL_DATA_CENTER, AWS, GCP, DIRECT)),

    CONNECTOR(Lists.newArrayList(SMTP, JENKINS, BAMBOO, SPLUNK, ELK, LOGZ, SUMO, APP_DYNAMICS, NEW_RELIC, ELB, SLACK,
        DOCKER, ECR, GCR, NEXUS, ARTIFACTORY, AMAZON_S3)),

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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends BaseYaml {
    private String name;
    private SettingValue.Yaml value;

    @lombok.Builder
    public Yaml(String name, SettingValue.Yaml value) {
      this.name = name;
      this.value = value;
    }
  }
}
