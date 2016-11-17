/**
 *
 */

package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private List<PipelineStage> pipelineStages = new ArrayList<>();
  private Map<String, Long> stateEtaMap = new HashMap<>();

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
          .withLastUpdatedAt(lastUpdatedAt);
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
      return pipeline;
    }
  }
}
