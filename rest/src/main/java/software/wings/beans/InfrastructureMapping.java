package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.mongodb.morphia.annotations.Entity;

/**
 * Created by anubhaw on 1/10/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "computeProviderType")
@Entity(value = "infrastructureMapping")
public abstract class InfrastructureMapping extends Base {
  private SettingAttribute computeProviderSettingId;
  private String envId;
  private String serviceTemplateId;
  private String computeProviderType;

  /**
   * Instantiates a new Infrastructure mapping.
   *
   * @param computeProviderType the compute provider type
   */
  public InfrastructureMapping(String computeProviderType) {
    this.computeProviderType = computeProviderType;
  }

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
   * Gets service template id.
   *
   * @return the service template id
   */
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
  public SettingAttribute getComputeProviderSettingId() {
    return computeProviderSettingId;
  }

  /**
   * Sets compute provider setting id.
   *
   * @param computeProviderSettingId the compute provider setting id
   */
  public void setComputeProviderSettingId(SettingAttribute computeProviderSettingId) {
    this.computeProviderSettingId = computeProviderSettingId;
  }
}
