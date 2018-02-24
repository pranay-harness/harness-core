/**
 *
 */

package software.wings.beans;

import static software.wings.beans.Pipeline.Builder.aPipeline;

import com.google.common.base.MoreObjects;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Transient;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * The Class Pipeline.
 *
 * @author Rishi
 */
@Entity(value = "pipelines", noClassnameStored = true)
public class Pipeline extends Base {
  @NotNull private String name;
  private String description;
  @NotNull private List<PipelineStage> pipelineStages = new ArrayList<>();
  private Map<String, Long> stateEtaMap = new HashMap<>();
  @Transient private List<Service> services = new ArrayList<>();
  @Transient private List<WorkflowExecution> workflowExecutions = new ArrayList<>();
  @Transient private boolean valid = true;
  @Transient private String validationMessage;
  @Transient private boolean templatized;
  @Embedded private List<FailureStrategy> failureStrategies = new ArrayList<>();

  /**
   * Gets state eta map.
   *
   * @return the state eta map
   */
  public Map<String, Long> getStateEtaMap() {
    return stateEtaMap;
  }

  /**
   * Sets state eta map.
   *
   * @param stateEtaMap the state eta map
   */
  public void setStateEtaMap(Map<String, Long> stateEtaMap) {
    this.stateEtaMap = stateEtaMap;
  }

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

  /**
   * Gets pipeline stages.
   *
   * @return the pipeline stages
   */
  public List<PipelineStage> getPipelineStages() {
    return pipelineStages;
  }

  /**
   * Sets pipeline stages.
   *
   * @param pipelineStages the pipeline stages
   */
  public void setPipelineStages(List<PipelineStage> pipelineStages) {
    this.pipelineStages = pipelineStages;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public boolean isTemplatized() {
    return templatized;
  }

  public void setTemplatized(boolean templatized) {
    this.templatized = templatized;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(name, description, pipelineStages, stateEtaMap);
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
    final Pipeline other = (Pipeline) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.description, other.description)
        && Objects.equals(this.pipelineStages, other.pipelineStages)
        && Objects.equals(this.stateEtaMap, other.stateEtaMap);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("description", description)
        .add("pipelineStages", pipelineStages)
        .add("stateEtaMap", stateEtaMap)
        .toString();
  }

  public List<Service> getServices() {
    return services;
  }

  public void setServices(List<Service> services) {
    this.services = services;
  }

  public List<WorkflowExecution> getWorkflowExecutions() {
    return workflowExecutions;
  }

  public void setWorkflowExecutions(List<WorkflowExecution> workflowExecutions) {
    this.workflowExecutions = workflowExecutions;
  }

  public Pipeline clone() {
    return aPipeline()
        .withAppId(getAppId())
        .withName(name)
        .withDescription(description)
        .withPipelineStages(getPipelineStages())
        .withFailureStrategies(getFailureStrategies())
        .withStateEtaMap(getStateEtaMap())
        .build();
  }

  public Builder toBuilder() {
    return aPipeline()
        .withName(getName())
        .withDescription(getDescription())
        .withPipelineStages(getPipelineStages())
        .withStateEtaMap(getStateEtaMap())
        .withFailureStrategies(getFailureStrategies())
        .withUuid(getUuid())
        .withAppId(getAppId())
        .withCreatedBy(getCreatedBy())
        .withCreatedAt(getCreatedAt())
        .withLastUpdatedBy(getLastUpdatedBy())
        .withLastUpdatedAt(getLastUpdatedAt());
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String description;
    private List<PipelineStage> pipelineStages = new ArrayList<>();
    private Map<String, Long> stateEtaMap = new HashMap<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private List<Service> services;
    private List<Variable> variables = new ArrayList<>();
    private List<FailureStrategy> failureStrategies = new ArrayList<>();

    private Builder() {}

    /**
     * A pipeline builder.
     *
     * @return the builder
     */
    public static Builder aPipeline() {
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

    /**
     * With pipeline stages builder.
     *
     * @param pipelineStages the pipeline stages
     * @return the builder
     */
    public Builder withPipelineStages(List<PipelineStage> pipelineStages) {
      this.pipelineStages = pipelineStages;
      return this;
    }

    /**
     * With state eta map builder.
     *
     * @param stateEtaMap the state eta map
     * @return the builder
     */
    public Builder withStateEtaMap(Map<String, Long> stateEtaMap) {
      this.stateEtaMap = stateEtaMap;
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
     * With services
     * @param services
     * @return
     */
    public Builder withServices(List<Service> services) {
      this.services = services;
      return this;
    }

    /**
     * With variables
     * @param variables
     * @return
     */
    public Builder withVariables(List<Variable> variables) {
      this.variables = variables;
      return this;
    }

    /**
     * With failureStrategies
     * @param failureStrategies
     * @return
     */
    public Builder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aPipeline()
          .withName(name)
          .withDescription(description)
          .withPipelineStages(pipelineStages)
          .withStateEtaMap(stateEtaMap)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withServices(services)
          .withVariables(variables)
          .withFailureStrategies(failureStrategies);
    }

    /**
     * Build pipeline.
     *
     * @return the pipeline
     */
    public Pipeline build() {
      Pipeline pipeline = new Pipeline();
      pipeline.setName(name);
      pipeline.setDescription(description);
      pipeline.setPipelineStages(pipelineStages);
      pipeline.setStateEtaMap(stateEtaMap);
      pipeline.setUuid(uuid);
      pipeline.setAppId(appId);
      pipeline.setCreatedBy(createdBy);
      pipeline.setCreatedAt(createdAt);
      pipeline.setLastUpdatedBy(lastUpdatedBy);
      pipeline.setLastUpdatedAt(lastUpdatedAt);
      pipeline.setServices(services);
      pipeline.setFailureStrategies(failureStrategies);
      return pipeline;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseEntityYaml {
    private String description;
    private List<PipelineStage.Yaml> pipelineStages = new ArrayList<>();
    private List<FailureStrategy.Yaml> failureStrategies;

    @lombok.Builder
    public Yaml(String harnessApiVersion, String description, List<PipelineStage.Yaml> pipelineStages) {
      super(EntityType.PIPELINE.name(), harnessApiVersion);
      this.description = description;
      this.pipelineStages = pipelineStages;
    }
  }
}
