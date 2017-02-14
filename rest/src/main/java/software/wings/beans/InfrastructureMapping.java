package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;

import java.util.Objects;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeInfo(use = Id.NAME, property = "infraMappingType")
@Entity(value = "infrastructureMapping")
public abstract class InfrastructureMapping extends Base {
  private String computeProviderSettingId;
  private String envId;
  private String serviceTemplateId;
  private String computeProviderType;
  private String infraMappingType;
  @Attributes(title = "Deployment type", required = true) private String deploymentType;
  @Attributes(title = "Connection Type") private String hostConnectionAttrs;
  @Transient @SchemaIgnore private String displayName;

  /**
   * The enum Infra mapping type.
   */
  public enum InfrastructureMappingType {
    /**
     * Physical data center ssh infra mapping type.
     */
    PHYSICAL_DATA_CENTER_SSH, /**
                               * Aws ssh infra mapping type.
                               */
    AWS_SSH, /**
              * Aws ecs infra mapping type.
              */
    AWS_ECS, /**
              * Aws kubernetes infra mapping type.
              */
    AWS_KUBERNETES
  }

  /**
   * Instantiates a new Infrastructure mapping.
   *
   * @param infraMappingType the infra mapping type
   */
  public InfrastructureMapping(String infraMappingType) {
    this.infraMappingType = infraMappingType;
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

  @SchemaIgnore
  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets host connection attrs.
   *
   * @return the host connection attrs
   */
  public String getHostConnectionAttrs() {
    return hostConnectionAttrs;
  }

  /**
   * Sets host connection attrs.
   *
   * @param hostConnectionAttrs the host connection attrs
   */
  public void setHostConnectionAttrs(String hostConnectionAttrs) {
    this.hostConnectionAttrs = hostConnectionAttrs;
  }

  /**
   * Gets deployment type.
   *
   * @return the deployment type
   */
  public String getDeploymentType() {
    return deploymentType;
  }

  /**
   * Sets deployment type.
   *
   * @param deploymentType the deployment type
   */
  public void setDeploymentType(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  /**
   * Gets infra mapping type.
   *
   * @return the infra mapping type
   */
  public String getInfraMappingType() {
    return infraMappingType;
  }

  /**
   * Sets infra mapping type.
   *
   * @param infraMappingType the infra mapping type
   */
  public void setInfraMappingType(String infraMappingType) {
    this.infraMappingType = infraMappingType;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("computeProviderSettingId", computeProviderSettingId)
        .add("envId", envId)
        .add("serviceTemplateId", serviceTemplateId)
        .add("computeProviderType", computeProviderType)
        .add("infraMappingType", infraMappingType)
        .add("deploymentType", deploymentType)
        .add("hostConnectionAttrs", hostConnectionAttrs)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(computeProviderSettingId, envId, serviceTemplateId, computeProviderType, infraMappingType,
              deploymentType, hostConnectionAttrs);
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
        && Objects.equals(this.hostConnectionAttrs, other.hostConnectionAttrs);
  }
}
