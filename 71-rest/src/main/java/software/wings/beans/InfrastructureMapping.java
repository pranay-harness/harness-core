package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.HarnessExportableEntity;
import io.harness.beans.EmbeddedUser;
import io.harness.data.validator.EntityName;
import io.harness.persistence.NameAccess;
import io.harness.persistence.PersistentIterable;
import io.harness.validation.Update;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.yaml.BaseEntityYaml;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeInfo(use = Id.NAME, property = "infraMappingType")
@Entity(value = "infrastructureMapping")
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("appId")
               , @Field("envId"), @Field("name") }))
@HarnessExportableEntity
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "InfrastructureMappingKeys")
public abstract class InfrastructureMapping extends Base implements EncryptableSetting, PersistentIterable, NameAccess {
  public static final String ENV_ID_KEY = "envId";
  public static final String NAME_KEY = "name";
  public static final String PROVISIONER_ID_KEY = "provisionerId";
  public static final String SERVICE_ID_KEY = "serviceId";
  public static final String INFRA_MAPPING_TYPE_KEY = "infraMappingType";

  @NotEmpty private String computeProviderSettingId;
  @NotEmpty private String envId;
  @NotEmpty private String serviceTemplateId;

  @NotEmpty(groups = {Update.class}) private String serviceId;

  @NotEmpty private String computeProviderType;
  @NotEmpty private String infraMappingType;
  @NotEmpty private String deploymentType;
  @SchemaIgnore private String computeProviderName;

  @EntityName private String name;

  // auto populate name
  @SchemaIgnore private boolean autoPopulate = true;

  @SchemaIgnore @NotEmpty private String accountId;

  @Nullable private String provisionerId;

  @Indexed private Long nextIteration;

  private Map<String, String> blueprints;

  /**
   * Instantiates a new Infrastructure mapping.
   */
  public InfrastructureMapping() {}
  /**
   * Instantiates a new Infrastructure mapping.
   *
   * @param infraMappingType the infra mapping type
   */
  public InfrastructureMapping(String infraMappingType) {
    this.infraMappingType = infraMappingType;
  }

  public InfrastructureMapping(String entityYamlPath, String appId, String accountId, String type, String uuid,
      EmbeddedUser createdBy, long createdAt, EmbeddedUser lastUpdatedBy, long lastUpdatedAt,
      String computeProviderSettingId, String envId, String serviceTemplateId, String serviceId,
      String computeProviderType, String infraMappingType, String deploymentType, String computeProviderName,
      String name, boolean autoPopulateName, Map<String, String> blueprints) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, entityYamlPath);
    this.computeProviderSettingId = computeProviderSettingId;
    this.envId = envId;
    this.serviceTemplateId = serviceTemplateId;
    this.serviceId = serviceId;
    this.computeProviderType = computeProviderType;
    this.infraMappingType = infraMappingType;
    this.deploymentType = deploymentType;
    this.computeProviderName = computeProviderName;
    this.name = name;
    this.autoPopulate = autoPopulateName;
    this.accountId = accountId;
    this.blueprints = blueprints;
  }

  public abstract void applyProvisionerVariables(Map<String, Object> map, NodeFilteringType nodeFilteringType);

  @SchemaIgnore
  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /**
   * Gets env id.
   *
   * @return the env id
   */
  @SchemaIgnore
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
   * Gets service template id.
   *
   * @return the service template id
   */
  @SchemaIgnore
  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  /**
   * Sets service template id.
   *
   * @param serviceTemplateId the service template id
   */
  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  /**
   * Gets compute provider type.
   *
   * @return the compute provider type
   */
  public String getComputeProviderType() {
    return computeProviderType;
  }

  /**
   * Sets compute provider type.
   *
   * @param computeProviderType the compute provider type
   */
  public void setComputeProviderType(String computeProviderType) {
    this.computeProviderType = computeProviderType;
  }

  /**
   * Gets compute provider setting id.
   *
   * @return the compute provider setting id
   */
  @SchemaIgnore
  public String getComputeProviderSettingId() {
    return computeProviderSettingId;
  }

  /**
   * Sets compute provider setting id.
   *
   * @param computeProviderSettingId the compute provider setting id
   */
  public void setComputeProviderSettingId(String computeProviderSettingId) {
    this.computeProviderSettingId = computeProviderSettingId;
  }

  @SchemaIgnore
  public String getComputeProviderName() {
    return computeProviderName;
  }

  public void setComputeProviderName(String computeProviderName) {
    this.computeProviderName = computeProviderName;
  }

  @SchemaIgnore
  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  @SchemaIgnore
  public boolean isAutoPopulate() {
    return autoPopulate;
  }

  public void setAutoPopulate(boolean autoPopulate) {
    this.autoPopulate = autoPopulate;
  }

  @SchemaIgnore
  @Override
  public String getAppId() {
    return super.getAppId();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getCreatedBy() {
    return super.getCreatedBy();
  }

  @SchemaIgnore
  @Override
  public EmbeddedUser getLastUpdatedBy() {
    return super.getLastUpdatedBy();
  }

  @SchemaIgnore
  @Override
  public long getCreatedAt() {
    return super.getCreatedAt();
  }

  @SchemaIgnore
  @Override
  public long getLastUpdatedAt() {
    return super.getLastUpdatedAt();
  }

  @SchemaIgnore
  @Override
  public String getUuid() {
    return super.getUuid();
  }

  public String getName() {
    return name;
  }

  @JsonIgnore @SchemaIgnore public abstract String getDefaultName();

  public void setName(String name) {
    this.name = name;
  }

  public String getDeploymentType() {
    return deploymentType;
  }

  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getInfraMappingType() {
    return infraMappingType;
  }

  public void setInfraMappingType(String infraMappingType) {
    this.infraMappingType = infraMappingType;
  }

  public String getProvisionerId() {
    return provisionerId;
  }

  public void setProvisionerId(String provisionerId) {
    this.provisionerId = provisionerId;
  }

  @SchemaIgnore
  public Map<String, String> getBlueprints() {
    return blueprints;
  }

  public void setBlueprints(Map<String, String> blueprints) {
    this.blueprints = blueprints;
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.INFRASTRUCTURE_MAPPING;
  }

  @JsonInclude(Include.NON_EMPTY) public abstract String getHostConnectionAttrs();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("computeProviderSettingId", computeProviderSettingId)
        .add("envId", envId)
        .add("serviceTemplateId", serviceTemplateId)
        .add("computeProviderType", computeProviderType)
        .add("infraMappingType", infraMappingType)
        .add("deploymentType", deploymentType)
        .add("provisionerId", provisionerId)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(computeProviderSettingId, envId, serviceTemplateId, computeProviderType, infraMappingType,
              deploymentType, provisionerId);
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
    final InfrastructureMapping other = (InfrastructureMapping) obj;
    return Objects.equals(this.computeProviderSettingId, other.computeProviderSettingId)
        && Objects.equals(this.envId, other.envId) && Objects.equals(this.serviceTemplateId, other.serviceTemplateId)
        && Objects.equals(this.computeProviderType, other.computeProviderType)
        && Objects.equals(this.infraMappingType, other.infraMappingType)
        && Objects.equals(this.deploymentType, other.deploymentType)
        && Objects.equals(this.provisionerId, other.provisionerId);
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class Yaml extends BaseEntityYaml {
    private String serviceName;
    private String infraMappingType;
    private String deploymentType;
    private Map<String, String> blueprints;

    public Yaml(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, Map<String, String> blueprints) {
      super(type, harnessApiVersion);
      this.serviceName = serviceName;
      this.infraMappingType = infraMappingType;
      this.deploymentType = deploymentType;
      this.blueprints = blueprints;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public abstract static class YamlWithComputeProvider extends Yaml {
    private String computeProviderType;
    private String computeProviderName;

    public YamlWithComputeProvider(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName, Map<String, String> blueprints) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType, blueprints);
      this.computeProviderType = computeProviderType;
      this.computeProviderName = computeProviderName;
    }
  }

  protected List<String> getList(Object input) {
    if (input instanceof String) {
      return Arrays.asList(((String) input).split(","));
    }

    return (List<String>) input;
  }
}
