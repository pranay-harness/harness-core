package software.wings.beans;

import static java.util.stream.Collectors.groupingBy;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.validator.EntityName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.annotation.Encryptable;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.stencils.DataProvider;
import software.wings.utils.validation.Update;
import software.wings.yaml.BaseEntityYaml;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeInfo(use = Id.NAME, property = "infraMappingType")
@Entity(value = "infrastructureMapping")
@Indexes(@Index(options = @IndexOptions(name = "yaml", unique = true),
    fields = { @Field("appId")
               , @Field("envId"), @Field("name") }))
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class InfrastructureMapping extends Base implements Encryptable {
  public static final String ENV_ID_KEY = "envId";
  public static final String NAME_KEY = "name";
  public static final String PROVISIONER_ID_KEY = "provisionerId";
  public static final String SERVICE_ID_KEY = "serviceId";

  @SchemaIgnore @NotEmpty private String computeProviderSettingId;
  @SchemaIgnore @NotEmpty private String envId;
  @SchemaIgnore @NotEmpty private String serviceTemplateId;

  @NotEmpty(groups = {Update.class}) @SchemaIgnore private String serviceId;

  @NotEmpty private String computeProviderType;
  @NotEmpty private String infraMappingType;
  @Attributes(title = "Deployment type", required = true) @NotEmpty private String deploymentType;
  @SchemaIgnore private String computeProviderName;

  @EntityName @Attributes(title = "Name") private String name;

  // auto populate name
  @SchemaIgnore private boolean autoPopulate = true;

  @SchemaIgnore @NotEmpty private String accountId;

  @Nullable @Indexed private String provisionerId;

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
      String name, List<String> keywords, boolean autoPopulateName) {
    super(uuid, appId, createdBy, createdAt, lastUpdatedBy, lastUpdatedAt, keywords, entityYamlPath);
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
  }

  public abstract void applyProvisionerVariables(Map<String, Object> map);

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

  @Singleton
  public static class HostConnectionAttributesDataProvider implements DataProvider {
    @Inject private SettingsService settingsService;
    @Inject private AppService appService;

    @Override
    public Map<String, String> getData(String appId, Map<String, String> params) {
      String accountId = appService.getAccountIdByAppId(appId);
      List<SettingAttribute> settingAttributes = settingsService.getFilteredGlobalSettingAttributesByType(accountId,
          SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES.name(), appId, params.get(EntityType.ENVIRONMENT.name()));

      Map<AccessType, List<SettingAttribute>> settingAttributeByType = settingAttributes.stream().collect(
          groupingBy(sa -> ((HostConnectionAttributes) sa.getValue()).getAccessType()));

      return Stream.of(KEY, USER_PASSWORD)
          .map(settingAttributeByType::get)
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .collect(Collectors.toMap(
              SettingAttribute::getUuid, SettingAttribute::getName, (v1, v2) -> v1, LinkedHashMap::new));
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends BaseEntityYaml {
    private String serviceName;
    private String infraMappingType;
    private String deploymentType;

    public Yaml(
        String type, String harnessApiVersion, String serviceName, String infraMappingType, String deploymentType) {
      super(type, harnessApiVersion);
      this.serviceName = serviceName;
      this.infraMappingType = infraMappingType;
      this.deploymentType = deploymentType;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class YamlWithComputeProvider extends Yaml {
    private String computeProviderType;
    private String computeProviderName;

    public YamlWithComputeProvider(String type, String harnessApiVersion, String serviceName, String infraMappingType,
        String deploymentType, String computeProviderType, String computeProviderName) {
      super(type, harnessApiVersion, serviceName, infraMappingType, deploymentType);
      this.computeProviderType = computeProviderType;
      this.computeProviderName = computeProviderName;
    }
  }
}
