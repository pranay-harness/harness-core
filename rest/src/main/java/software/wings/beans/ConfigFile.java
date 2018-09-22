package software.wings.beans;

import static software.wings.beans.EntityVersion.Builder.anEntityVersion;

import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.validation.Create;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.beans.yaml.YamlType;
import software.wings.security.EncryptionType;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.ws.rs.DefaultValue;

/**
 * Created by anubhaw on 4/12/16.
 */
@Entity(value = "configFiles", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("entityId")
      , @Field("templateId"), @Field("relativeFilePath"), @Field("configOverrideType"), @Field("instances"),
          @Field("configOverrideExpression")
    },
    options = @IndexOptions(
        unique = true, name = "entityId_1_templateId_1_relativeFilePath_1_OType_1_instances_1_OExpression_1")))
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigFile extends BaseFile implements Encryptable {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  @NotEmpty private String accountId;

  @FormDataParam("templateId") @DefaultValue(DEFAULT_TEMPLATE_ID) private String templateId;

  @FormDataParam("envId") @NotEmpty(groups = {Create.class}) private String envId;

  @FormDataParam("entityType") @NotNull(groups = {Create.class}) private EntityType entityType;

  @FormDataParam("entityId") @NotEmpty(groups = {Create.class}) private String entityId;

  @FormDataParam("description") private String description;

  @FormDataParam("parentConfigFileId") private String parentConfigFileId;

  @FormDataParam("relativeFilePath") private String relativeFilePath;

  @FormDataParam("targetToAllEnv") private boolean targetToAllEnv;

  @FormDataParam("defaultVersion") private int defaultVersion;

  @Default private Map<String, EntityVersion> envIdVersionMap = Maps.newHashMap();

  @JsonIgnore @FormDataParam("envIdVersionMapString") private String envIdVersionMapString;

  @Transient @FormDataParam("setAsDefault") private boolean setAsDefault;

  @Transient @FormDataParam("notes") private String notes;

  private String overridePath;

  @NotNull(groups = {Create.class}) @FormDataParam("configOverrideType") private ConfigOverrideType configOverrideType;
  @FormDataParam("configOverrideExpression") private String configOverrideExpression;

  @FormDataParam("instances") private List<String> instances;

  @Transient private ConfigFile overriddenConfigFile;

  @FormDataParam("encrypted") private boolean encrypted;

  @FormDataParam("encryptedFileId") private String encryptedFileId;

  @SchemaIgnore @Transient private String secretFileName;

  @SchemaIgnore @Transient private String serviceId;

  @SchemaIgnore @Transient private transient EncryptionType encryptionType;

  @SchemaIgnore @Transient private transient String encryptedBy;

  /**
   * Gets version for env.
   *
   * @param envId the env id
   * @return the version for env
   */
  @JsonIgnore
  public int getVersionForEnv(String envId) {
    EntityVersion defualtVersion = anEntityVersion().withVersion(defaultVersion).build();
    if (envIdVersionMap == null || envIdVersionMap.get(envId) == null) {
      return defualtVersion.getVersion();
    }

    return envIdVersionMap.get(envId).getVersion();
  }

  /**
   * The enum Config override type.
   */
  public enum ConfigOverrideType {
    /**
     * All config override type.
     */
    ALL,
    /**
     * Instances config override type.
     */
    INSTANCES,
    /**
     * Custom config override type.
     */
    CUSTOM
  }

  public ConfigFile cloneInternal() {
    ConfigFile configFile = ConfigFile.builder()
                                .envId(getEnvId())
                                .entityType(getEntityType())
                                .entityId(getEntityId())
                                .templateId(getTemplateId())
                                .relativeFilePath(getRelativeFilePath())
                                .targetToAllEnv(isTargetToAllEnv())
                                .encrypted(isEncrypted())
                                .accountId(getAccountId())
                                .configOverrideExpression(getConfigOverrideExpression())
                                .configOverrideType(getConfigOverrideType())
                                .encryptedFileId(getEncryptedFileId())
                                .build();
    configFile.setAppId(getAppId());
    configFile.setFileName(getFileName());
    return configFile;
  }

  @Override
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.CONFIG_FILE;
  }

  public void setSettingType(SettingVariableTypes type) {
    //
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public List<java.lang.reflect.Field> getEncryptedFields() {
    return Collections.emptyList();
  }

  @Override
  public boolean isDecrypted() {
    return false;
  }

  @Override
  public void setDecrypted(boolean decrypted) {
    //
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class Yaml extends BaseEntityYaml {
    private String targetFilePath;
    private boolean encrypted;
    private String fileName;
    private String description;
    private String checksum;
    private String checksumType;
    private boolean targetToAllEnv;
    private List<String> targetEnvs = new ArrayList<>();

    public Yaml(String harnessApiVersion) {
      super(YamlType.CONFIG_FILE.name(), harnessApiVersion);
    }

    @Builder
    public Yaml(String harnessApiVersion, String targetFilePath, boolean encrypted, String fileName, String description,
        String checksum, String checksumType, boolean targetToAllEnv, List<String> targetEnvs) {
      super(YamlType.CONFIG_FILE.name(), harnessApiVersion);
      this.targetFilePath = targetFilePath;
      this.encrypted = encrypted;
      this.fileName = fileName;
      this.description = description;
      this.checksum = checksum;
      this.checksumType = checksumType;
      this.targetToAllEnv = targetToAllEnv;
      this.targetEnvs = targetEnvs;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static class OverrideYaml extends BaseEntityYaml {
    private String serviceName;
    private String targetFilePath;
    private String fileName;
    private String checksum;
    private String checksumType;
    private boolean encrypted;

    public OverrideYaml(String harnessApiVersion) {
      super(YamlType.CONFIG_FILE_OVERRIDE.name(), harnessApiVersion);
    }

    @Builder
    public OverrideYaml(String harnessApiVersion, String serviceName, String targetFilePath, String fileName,
        String checksum, String checksumType, boolean encrypted) {
      super(YamlType.CONFIG_FILE_OVERRIDE.name(), harnessApiVersion);
      this.serviceName = serviceName;
      this.targetFilePath = targetFilePath;
      this.fileName = fileName;
      this.checksum = checksum;
      this.checksumType = checksumType;
      this.encrypted = encrypted;
    }
  }
}
