package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Arrays.stream;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.settings.SettingValue.SettingVariableTypes.AMAZON_S3;
import static software.wings.settings.SettingValue.SettingVariableTypes.AMAZON_S3_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.APM_VERIFICATION;
import static software.wings.settings.SettingValue.SettingVariableTypes.APP_DYNAMICS;
import static software.wings.settings.SettingValue.SettingVariableTypes.ARTIFACTORY;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE;
import static software.wings.settings.SettingValue.SettingVariableTypes.AZURE_ARTIFACTS_PAT;
import static software.wings.settings.SettingValue.SettingVariableTypes.BAMBOO;
import static software.wings.settings.SettingValue.SettingVariableTypes.BASTION_HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.BUG_SNAG;
import static software.wings.settings.SettingValue.SettingVariableTypes.CE_AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.CE_GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.CUSTOM;
import static software.wings.settings.SettingValue.SettingVariableTypes.DATA_DOG;
import static software.wings.settings.SettingValue.SettingVariableTypes.DOCKER;
import static software.wings.settings.SettingValue.SettingVariableTypes.DYNA_TRACE;
import static software.wings.settings.SettingValue.SettingVariableTypes.ECR;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELB;
import static software.wings.settings.SettingValue.SettingVariableTypes.ELK;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCR;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCS;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCS_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.GIT;
import static software.wings.settings.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.settings.SettingValue.SettingVariableTypes.HTTP_HELM_REPO;
import static software.wings.settings.SettingValue.SettingVariableTypes.INSTANA;
import static software.wings.settings.SettingValue.SettingVariableTypes.JENKINS;
import static software.wings.settings.SettingValue.SettingVariableTypes.JIRA;
import static software.wings.settings.SettingValue.SettingVariableTypes.KUBERNETES_CLUSTER;
import static software.wings.settings.SettingValue.SettingVariableTypes.LOGZ;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEW_RELIC;
import static software.wings.settings.SettingValue.SettingVariableTypes.NEXUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.PCF;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.settings.SettingValue.SettingVariableTypes.PROMETHEUS;
import static software.wings.settings.SettingValue.SettingVariableTypes.SERVICENOW;
import static software.wings.settings.SettingValue.SettingVariableTypes.SFTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SLACK;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMB;
import static software.wings.settings.SettingValue.SettingVariableTypes.SMTP;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPLUNK;
import static software.wings.settings.SettingValue.SettingVariableTypes.SPOT_INST;
import static software.wings.settings.SettingValue.SettingVariableTypes.STRING;
import static software.wings.settings.SettingValue.SettingVariableTypes.SUMO;
import static software.wings.settings.SettingValue.SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.data.validator.Trimmed;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.Field;
import io.harness.mongo.index.Index;
import io.harness.mongo.index.IndexOptions;
import io.harness.mongo.index.Indexed;
import io.harness.mongo.index.Indexes;
import io.harness.persistence.NameAccess;
import io.harness.security.encryption.EncryptionType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.artifact.ArtifactStreamSummary;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.settings.validation.ConnectivityValidationAttributes;
import software.wings.yaml.BaseYaml;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.validation.Valid;

/**
 * Created by anubhaw on 5/16/16.
 */
@OwnedBy(CDC)
@Indexes({
  @Index(name = "locate", options = @IndexOptions(unique = true),
      fields = { @Field("accountId")
                 , @Field("appId"), @Field("envId"), @Field("name"), @Field("value.type") })
  ,
      @Index(name = "acctCatTypeIdx", fields = { @Field("accountId")
                                                 , @Field("category"), @Field("value.type") }),
      @Index(name = "acctValTypeIdx", fields = { @Field("accountId")
                                                 , @Field("value.type") }),
      @Index(name = "value.type_1_nextIteration_1", fields = { @Field("value.type")
                                                               , @Field("nextIteration") }),
      @Index(name = "secretsMigrationIdx", fields = { @Field("value.type")
                                                      , @Field("nextSecretMigrationIteration") }),
      @Index(name = "secretsMigrationPerAccountIdx", fields = {
        @Field("value.type"), @Field("secretsMigrated"), @Field("accountId")
      })
})
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "SettingAttributeKeys")
@Entity(value = "settingAttributes")
@HarnessEntity(exportable = true)
public class SettingAttribute extends Base implements NameAccess, PersistentRegularIterable {
  public static final String CATEGORY_KEY = "category";
  public static final String ENV_ID_KEY = "envId";
  public static final String NAME_KEY = "name";
  public static final String VALUE_TYPE_KEY = "value.type";

  @NotEmpty private String envId = GLOBAL_ENV_ID;
  @NotEmpty String accountId;
  @NotEmpty @EntityName @Trimmed private String name;
  @Valid private SettingValue value;
  @Valid @Transient private ConnectivityValidationAttributes validationAttributes;
  private SettingCategory category = SettingCategory.SETTING;
  private List<String> appIds;
  private UsageRestrictions usageRestrictions;
  private transient long artifactStreamCount;
  private transient List<ArtifactStreamSummary> artifactStreams;
  private boolean sample;

  @Indexed private Long nextIteration;
  private Long nextSecretMigrationIteration;
  private boolean secretsMigrated;
  private String connectivityError;

  @SchemaIgnore @Transient private transient EncryptionType encryptionType;

  @SchemaIgnore @Transient private transient String encryptedBy;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (SettingAttributeKeys.nextIteration.equals(fieldName)) {
      return nextIteration;
    }
    if (SettingAttributeKeys.nextSecretMigrationIteration.equals(fieldName)) {
      return nextSecretMigrationIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    if (SettingAttributeKeys.nextIteration.equals(fieldName)) {
      this.nextIteration = nextIteration;
      return;
    }
    if (SettingAttributeKeys.nextSecretMigrationIteration.equals(fieldName)) {
      this.nextSecretMigrationIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public enum SettingCategory {
    CLOUD_PROVIDER(Lists.newArrayList(PHYSICAL_DATA_CENTER, AWS, AZURE, GCP, KUBERNETES_CLUSTER, PCF, SPOT_INST)),

    CONNECTOR(Lists.newArrayList(SMTP, JENKINS, BAMBOO, SPLUNK, ELK, LOGZ, SUMO, APP_DYNAMICS, INSTANA, NEW_RELIC,
        DYNA_TRACE, BUG_SNAG, DATA_DOG, APM_VERIFICATION, PROMETHEUS, ELB, SLACK, DOCKER, ECR, GCR, NEXUS, ARTIFACTORY,
        AMAZON_S3, GCS, GIT, SMB, JIRA, SFTP, SERVICENOW, CUSTOM)),

    SETTING(Lists.newArrayList(
        HOST_CONNECTION_ATTRIBUTES, BASTION_HOST_CONNECTION_ATTRIBUTES, STRING, WINRM_CONNECTION_ATTRIBUTES)),

    HELM_REPO(Lists.newArrayList(HTTP_HELM_REPO, AMAZON_S3_HELM_REPO, GCS_HELM_REPO)),

    AZURE_ARTIFACTS(Lists.newArrayList(AZURE_ARTIFACTS_PAT)),

    CE_CONNECTOR(Lists.newArrayList(CE_AWS, CE_GCP));

    @Getter private List<SettingVariableTypes> settingVariableTypes;

    SettingCategory(List<SettingVariableTypes> settingVariableTypes) {
      this.settingVariableTypes = settingVariableTypes;
    }

    public static SettingCategory getCategory(SettingVariableTypes settingVariableType) {
      return stream(SettingCategory.values())
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
    private ConnectivityValidationAttributes connectivityValidationAttributes;
    private SettingCategory category = SettingCategory.SETTING;
    private List<String> appIds;
    private String uuid;
    private String appId = GLOBAL_APP_ID;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private UsageRestrictions usageRestrictions;
    private boolean sample;
    private String connectivityError;

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

    public Builder withCategory(SettingCategory category) {
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

    public Builder withUsageRestrictions(UsageRestrictions usageRestrictions) {
      this.usageRestrictions = usageRestrictions;
      return this;
    }

    public Builder withConnectivityValidationAttributes(
        ConnectivityValidationAttributes connectivityValidationAttributes) {
      this.connectivityValidationAttributes = connectivityValidationAttributes;
      return this;
    }

    public Builder withSample(boolean sample) {
      this.sample = sample;
      return this;
    }

    public Builder withConnectivityError(String connectivityError) {
      this.connectivityError = connectivityError;
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
          .withLastUpdatedAt(lastUpdatedAt)
          .withUsageRestrictions(usageRestrictions)
          .withConnectivityValidationAttributes(connectivityValidationAttributes)
          .withSample(sample);
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
      settingAttribute.setUsageRestrictions(usageRestrictions);
      settingAttribute.setValidationAttributes(connectivityValidationAttributes);
      settingAttribute.setSample(sample);
      settingAttribute.setConnectivityError(connectivityError);
      return settingAttribute;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static class Yaml extends BaseYaml {
    private String name;
    private SettingValue.Yaml value;

    @lombok.Builder
    public Yaml(String name, SettingValue.Yaml value) {
      this.name = name;
      this.value = value;
    }
  }

  @UtilityClass
  public static final class SettingAttributeKeys {
    // Temporary
    public static final String appId = "appId";
    public static final String createdAt = "createdAt";
    public static final String uuid = "uuid";
    public static final String accountId = "accountId";
    public static final String valueType = SettingAttributeKeys.value + ".type";
    public static final String isCEEnabled = SettingAttributeKeys.value + ".ccmConfig.cloudCostEnabled";
  }

  @Nonnull
  public List<String> fetchRelevantSecretIds() {
    return value == null ? Collections.emptyList() : value.fetchRelevantEncryptedSecrets();
  }
}
