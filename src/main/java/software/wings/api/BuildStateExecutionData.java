package software.wings.api;

import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateExecutionData;

/**
 * Created by anubhaw on 10/26/16.
 */
public class BuildStateExecutionData extends StateExecutionData {
  private String artifactStreamId;
  private String artifactName;
  private String artifactId;

  /**
   * Gets artifact stream id.
   *
   * @return the artifact stream id
   */
  public String getArtifactStreamId() {
    return artifactStreamId;
  }

  /**
   * Sets artifact stream id.
   *
   * @param artifactStreamId the artifact stream id
   */
  public void setArtifactStreamId(String artifactStreamId) {
    this.artifactStreamId = artifactStreamId;
  }

  /**
   * Gets artifact name.
   *
   * @return the artifact name
   */
  public String getArtifactName() {
    return artifactName;
  }

  /**
   * Sets artifact name.
   *
   * @param artifactName the artifact name
   */
  public void setArtifactName(String artifactName) {
    this.artifactName = artifactName;
  }

  /**
   * Gets artifact id.
   *
   * @return the artifact id
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Sets artifact id.
   *
   * @param artifactId the artifact id
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String artifactStreamId;
    private String artifactName;
    private String artifactId;
    private String stateName;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private String errorMsg;

    private Builder() {}

    /**
     * A build state execution data builder.
     *
     * @return the builder
     */
    public static Builder aBuildStateExecutionData() {
      return new Builder();
    }

    /**
     * With artifact stream id builder.
     *
     * @param artifactStreamId the artifact stream id
     * @return the builder
     */
    public Builder withArtifactStreamId(String artifactStreamId) {
      this.artifactStreamId = artifactStreamId;
      return this;
    }

    /**
     * With artifact name builder.
     *
     * @param artifactName the artifact name
     * @return the builder
     */
    public Builder withArtifactName(String artifactName) {
      this.artifactName = artifactName;
      return this;
    }

    /**
     * With artifact id builder.
     *
     * @param artifactId the artifact id
     * @return the builder
     */
    public Builder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    /**
     * With state name builder.
     *
     * @param stateName the state name
     * @return the builder
     */
    public Builder withStateName(String stateName) {
      this.stateName = stateName;
      return this;
    }

    /**
     * With start ts builder.
     *
     * @param startTs the start ts
     * @return the builder
     */
    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts builder.
     *
     * @param endTs the end ts
     * @return the builder
     */
    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status builder.
     *
     * @param status the status
     * @return the builder
     */
    public Builder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * With error msg builder.
     *
     * @param errorMsg the error msg
     * @return the builder
     */
    public Builder withErrorMsg(String errorMsg) {
      this.errorMsg = errorMsg;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aBuildStateExecutionData()
          .withArtifactStreamId(artifactStreamId)
          .withArtifactName(artifactName)
          .withArtifactId(artifactId)
          .withStateName(stateName)
          .withStartTs(startTs)
          .withEndTs(endTs)
          .withStatus(status)
          .withErrorMsg(errorMsg);
    }

    /**
     * Build build state execution data.
     *
     * @return the build state execution data
     */
    public BuildStateExecutionData build() {
      BuildStateExecutionData buildStateExecutionData = new BuildStateExecutionData();
      buildStateExecutionData.setArtifactStreamId(artifactStreamId);
      buildStateExecutionData.setArtifactName(artifactName);
      buildStateExecutionData.setArtifactId(artifactId);
      buildStateExecutionData.setStateName(stateName);
      buildStateExecutionData.setStartTs(startTs);
      buildStateExecutionData.setEndTs(endTs);
      buildStateExecutionData.setStatus(status);
      buildStateExecutionData.setErrorMsg(errorMsg);
      return buildStateExecutionData;
    }
  }
}
