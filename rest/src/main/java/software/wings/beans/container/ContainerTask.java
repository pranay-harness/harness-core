package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import software.wings.beans.DeploymentSpecification;
import software.wings.beans.EmbeddedUser;

import java.util.List;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deploymentType")
@Indexes(@Index(fields = { @Field("serviceId")
                           , @Field("deploymentType") }, options = @IndexOptions(unique = true)))
@Entity("containerTasks")
public abstract class ContainerTask extends DeploymentSpecification {
  static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";
  static final String CONTAINER_NAME_PLACEHOLDER_REGEX = "\\$\\{CONTAINER_NAME}";
  static final String SECRET_NAME_PLACEHOLDER_REGEX = "\\$\\{SECRET_NAME}";

  static final String SERVICE_VAR_PLACEHOLDER_REGEX = "\\$\\{serviceVariable\\.[^.}]*}";
  static final String WORKFLOW_VAR_PLACEHOLDER_REGEX = "\\$\\{workflow\\.variables\\.[^.}]*}";
  static final String DUMMY_DOCKER_IMAGE_NAME = "hv--docker-image-name--hv";
  static final String DUMMY_CONTAINER_NAME = "hv--container-name--hv";
  static final String DUMMY_SECRET_NAME = "hv--secret-name--hv";
  static final String DUMMY_SERVICE_VAR = "\"hv--service--var--hv\"";
  static final String DUMMY_WORKFLOW_VAR = "\"hv--workflow--var--hv\"";

  @NotEmpty private String deploymentType;
  @SchemaIgnore @NotEmpty private String serviceId;

  private AdvancedType advancedType;
  private String advancedConfig;

  private List<ContainerDefinition> containerDefinitions;

  public ContainerTask(String deploymentType) {
    this.deploymentType = deploymentType;
  }

  public List<ContainerDefinition> getContainerDefinitions() {
    return containerDefinitions;
  }

  public void setContainerDefinitions(List<ContainerDefinition> containerDefinitions) {
    this.containerDefinitions = containerDefinitions;
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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends DeploymentSpecification.Yaml {
    private String advancedType;
    private String advancedConfig;
    private ContainerDefinition.Yaml containerDefinition;

    protected Yaml(String type, String harnessApiVersion, String advancedType, String advancedConfig,
        ContainerDefinition.Yaml containerDefinition) {
      super(type, harnessApiVersion);
      this.advancedType = advancedType;
      this.advancedConfig = advancedConfig;
      this.containerDefinition = containerDefinition;
    }
  }
}
