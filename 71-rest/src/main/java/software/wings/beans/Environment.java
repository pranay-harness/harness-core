package software.wings.beans;

import static java.util.Arrays.asList;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.yaml.YamlHelper.trimYaml;

import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
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
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Environment bean class.
 *
 * @author Rishi
 */
@Entity(value = "environments", noClassnameStored = true)
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true), fields = { @Field("appId")
                                                                                  , @Field("name") }))
@Data
@EqualsAndHashCode(callSuper = false)
public class Environment extends Base {
  public static final String NAME_KEY = "name";
  public static final String ENVIRONMENT_TYPE_KEY = "environmentType";

  @NotEmpty @EntityName private String name;
  private String description;
  private String configMapYaml;
  private String helmValueYaml;
  private Map<String, String> configMapYamlByServiceTemplateId;
  private Map<String, String> helmValueYamlByServiceTemplateId;
  @NotNull private EnvironmentType environmentType = NON_PROD;
  @Transient private List<ServiceTemplate> serviceTemplates;
  @Transient private List<ConfigFile> configFiles;
  @Transient private Setup setup;

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
   * Gets description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets description.
   *
   * @param description the description
   */
  public void setDescription(String description) {
    this.description = description;
  }

  public String getConfigMapYaml() {
    return configMapYaml;
  }

  public void setConfigMapYaml(String configMapYaml) {
    this.configMapYaml = trimYaml(configMapYaml);
  }

  public Map<String, String> getConfigMapYamlByServiceTemplateId() {
    return configMapYamlByServiceTemplateId;
  }

  public void setConfigMapYamlByServiceTemplateId(Map<String, String> configMapYamlByServiceTemplateId) {
    this.configMapYamlByServiceTemplateId = configMapYamlByServiceTemplateId;
  }

  public String getHelmValueYaml() {
    return helmValueYaml;
  }

  public void setHelmValueYaml(String helmValueYaml) {
    this.helmValueYaml = helmValueYaml;
  }

  public Map<String, String> getHelmValueYamlByServiceTemplateId() {
    return helmValueYamlByServiceTemplateId;
  }

  public void setHelmValueYamlByServiceTemplateId(Map<String, String> helmValueYamlByServiceTemplateId) {
    this.helmValueYamlByServiceTemplateId = helmValueYamlByServiceTemplateId;
  }

  /**
   * Gets environment type.
   *
   * @return the environment type
   */
  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  /**
   * Sets environment type.
   *
   * @param environmentType the environment type
   */
  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  /**
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  /**
   * Sets config files.
   *
   * @param configFiles the config files
   */
  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  /**
   * Gets service templates.
   *
   * @return the service templates
   */
  public List<ServiceTemplate> getServiceTemplates() {
    return serviceTemplates;
  }

  /**
   * Sets service templates.
   *
   * @param serviceTemplates the service templates
   */
  public void setServiceTemplates(List<ServiceTemplate> serviceTemplates) {
    this.serviceTemplates = serviceTemplates;
  }

  /**
   * Gets setup.
   *
   * @return the setup
   */
  public Setup getSetup() {
    return setup;
  }

  /**
   * Sets setup.
   *
   * @param setup the setup
   */
  public void setSetup(Setup setup) {
    this.setup = setup;
  }

  public Environment cloneInternal() {
    return anEnvironment()
        .withName(getName())
        .withAppId(getAppId())
        .withDescription(getDescription())
        .withConfigMapYaml(getConfigMapYaml())
        .withConfigMapYamlByServiceTemplateId(getConfigMapYamlByServiceTemplateId())
        .withHelmValueYaml(getHelmValueYaml())
        .withHelmValueYamlByServiceTemplateId(getHelmValueYamlByServiceTemplateId())
        .withEnvironmentType(getEnvironmentType())
        .build();
  }

  @Override
  public List<Object> generateKeywords() {
    List<Object> keywords = new ArrayList<>();
    keywords.addAll(asList(name, description, environmentType));
    keywords.addAll(super.generateKeywords());
    return keywords;
  }

  /**
   * The enum Environment type.
   */
  public enum EnvironmentType {
    /**
     * Prod environment type.
     */
    PROD,
    /**
     * Non prod environment type.
     */
    NON_PROD,
    /**
     * All environment type.
     */
    ALL,
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private String configMapYaml;
    private Map<String, String> configMapYamlByServiceTemplateId;
    private String helmValueYaml;
    private Map<String, String> helmValueYamlByServiceTemplateId;
    private EnvironmentType environmentType = NON_PROD;
    private List<ConfigFile> configFiles;
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private Builder() {}

    /**
     * An environment builder.
     *
     * @return the builder
     */
    public static Builder anEnvironment() {
      return new Builder();
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
     * With description builder.
     *
     * @param description the description
     * @return the builder
     */
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withConfigMapYaml(String configMapYaml) {
      this.configMapYaml = configMapYaml;
      return this;
    }

    public Builder withConfigMapYamlByServiceTemplateId(Map<String, String> configMapYamlByServiceTemplateId) {
      this.configMapYamlByServiceTemplateId = configMapYamlByServiceTemplateId;
      return this;
    }

    public Builder withHelmValueYaml(String helmValueYaml) {
      this.helmValueYaml = helmValueYaml;
      return this;
    }

    public Builder withHelmValueYamlByServiceTemplateId(Map<String, String> helmValueYamlByServiceTemplateId) {
      this.helmValueYamlByServiceTemplateId = helmValueYamlByServiceTemplateId;
      return this;
    }

    /**
     * With environment type builder.
     *
     * @param environmentType the environment type
     * @return the builder
     */
    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFiles the config files
     * @return the builder
     */
    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
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
      return anEnvironment()
          .withName(name)
          .withDescription(description)
          .withConfigMapYaml(configMapYaml)
          .withConfigMapYamlByServiceTemplateId(configMapYamlByServiceTemplateId)
          .withHelmValueYaml(helmValueYaml)
          .withHelmValueYamlByServiceTemplateId(helmValueYamlByServiceTemplateId)
          .withEnvironmentType(environmentType)
          .withConfigFiles(configFiles)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt);
    }

    /**
     * Build environment.
     *
     * @return the environment
     */
    public Environment build() {
      Environment environment = new Environment();
      environment.setName(name);
      environment.setDescription(description);
      environment.setConfigMapYaml(configMapYaml);
      environment.setConfigMapYamlByServiceTemplateId(configMapYamlByServiceTemplateId);
      environment.setHelmValueYaml(helmValueYaml);
      environment.setHelmValueYamlByServiceTemplateId(helmValueYamlByServiceTemplateId);
      environment.setEnvironmentType(environmentType);
      environment.setConfigFiles(configFiles);
      environment.setUuid(uuid);
      environment.setAppId(appId);
      environment.setCreatedBy(createdBy);
      environment.setCreatedAt(createdAt);
      environment.setLastUpdatedBy(lastUpdatedBy);
      environment.setLastUpdatedAt(lastUpdatedAt);
      return environment;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private String configMapYaml;
    private Map<String, String> configMapYamlByServiceTemplateId;
    private String helmValueYaml;
    private Map<String, String> helmValueYamlByServiceTemplateId;
    private String environmentType = "NON_PROD";
    private List<VariableOverrideYaml> variableOverrides = new ArrayList<>();

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, String configMapYaml,
        Map<String, String> configMapYamlByServiceTemplateId, String helmValueYaml,
        Map<String, String> helmValueYamlByServiceTemplateId, String environmentType,
        List<VariableOverrideYaml> variableOverrides) {
      super(EntityType.ENVIRONMENT.name(), harnessApiVersion);
      this.description = description;
      this.configMapYaml = configMapYaml;
      this.configMapYamlByServiceTemplateId = configMapYamlByServiceTemplateId;
      this.helmValueYaml = helmValueYaml;
      this.helmValueYamlByServiceTemplateId = helmValueYamlByServiceTemplateId;
      this.environmentType = environmentType;
      this.variableOverrides = variableOverrides;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class VariableOverrideYaml extends NameValuePair.AbstractYaml {
    private String serviceName;

    @lombok.Builder
    public VariableOverrideYaml(String name, String value, String valueType, String serviceName) {
      super(name, value, valueType);
      this.serviceName = serviceName;
    }
  }
}
