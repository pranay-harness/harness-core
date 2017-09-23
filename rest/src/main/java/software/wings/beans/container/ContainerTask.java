package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;
import software.wings.beans.EmbeddedUser;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deploymentType")
@Indexes(@Index(fields = { @Field("serviceId")
                           , @Field("deploymentType") }, options = @IndexOptions(unique = true)))
@Entity("containerTasks")
public abstract class ContainerTask extends Base {
  @NotEmpty private String deploymentType;
  @SchemaIgnore @NotEmpty private String serviceId;

  private AdvancedType advancedType;
  private String advancedConfig;

  public ContainerTask(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public String getDeploymentType() {
    return deploymentType;
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public AdvancedType getAdvancedType() {
    return advancedType;
  }

  public void setAdvancedType(AdvancedType advancedType) {
    this.advancedType = advancedType;
  }

  public String getAdvancedConfig() {
    return advancedConfig;
  }

  public void setAdvancedConfig(String advancedConfig) {
    this.advancedConfig = advancedConfig;
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

  public abstract ContainerTask convertToAdvanced();

  public abstract ContainerTask convertFromAdvanced();

  public abstract void validateAdvanced();

  public enum AdvancedType { JSON, YAML }
}
