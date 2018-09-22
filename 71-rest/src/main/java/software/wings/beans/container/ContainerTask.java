package software.wings.beans.container;

import static software.wings.yaml.YamlHelper.trimYaml;

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
import java.util.regex.Pattern;

/**
 * Created by anubhaw on 2/6/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "deploymentType")
@Indexes(@Index(options = @IndexOptions(name = "service", unique = true),
    fields = { @Field("serviceId")
               , @Field("deploymentType") }))
@Entity("containerTasks")
public abstract class ContainerTask extends DeploymentSpecification {
  static final String DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX = "\\$\\{DOCKER_IMAGE_NAME}";
  static final String DOCKER_IMAGE_NAME_REGEX = "(\\s*\"?image\"?\\s*:\\s*\"?)";
  static final String CONTAINER_NAME_PLACEHOLDER_REGEX = "\\$\\{CONTAINER_NAME}";

  static final String DUMMY_DOCKER_IMAGE_NAME = "hv--docker-image-name--hv";
  static final String DUMMY_CONTAINER_NAME = "hv--container-name--hv";

  @NotEmpty private String deploymentType;
  @SchemaIgnore @NotEmpty private String serviceId;

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

  public String getAdvancedConfig() {
    return advancedConfig;
  }

  public void setAdvancedConfig(String advancedConfig) {
    this.advancedConfig = trimYaml(advancedConfig);
  }

  public static Pattern compileRegexPattern(String domainName) {
    return Pattern.compile(
        DOCKER_IMAGE_NAME_REGEX + "(" + Pattern.quote(domainName) + "\\/)" + DOCKER_IMAGE_NAME_PLACEHOLDER_REGEX);
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

  protected void copyConfigToContainerTask(ContainerTask newContainerTask) {
    newContainerTask.setAdvancedConfig(this.getAdvancedConfig());
    newContainerTask.setContainerDefinitions(this.getContainerDefinitions());
    newContainerTask.setServiceId(this.getServiceId());
    newContainerTask.setAppId(this.getAppId());
  }

  public ContainerTask cloneInternal() {
    throw new UnsupportedOperationException();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends DeploymentSpecification.Yaml {
    private String advancedConfig;
    private ContainerDefinition.Yaml containerDefinition;

    protected Yaml(
        String type, String harnessApiVersion, String advancedConfig, ContainerDefinition.Yaml containerDefinition) {
      super(type, harnessApiVersion);
      this.advancedConfig = advancedConfig;
      this.containerDefinition = containerDefinition;
    }
  }
}
