package software.wings.sm;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Created by sgurubelli on 7/26/17.
 */
public class PipelineSummary {
  private String pipelineId;
  private String pipelineName;

  /**
   * Get PiplineId
   *
   * @return
   */
  public String getPipelineId() {
    return pipelineId;
  }

  /**
   * Set pipeline Id
   *
   * @param pipelineId
   */
  public void setPipelineId(String pipelineId) {
    this.pipelineId = pipelineId;
  }

  /**
   * Get Pipline Name
   * @return
   */
  public String getPipelineName() {
    return pipelineName;
  }

  /**
   * Set Pipline Name
   * @param pipelineName
   */
  public void setPipelineName(String pipelineName) {
    this.pipelineName = pipelineName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PipelineSummary that = (PipelineSummary) o;
    return Objects.equals(pipelineId, that.pipelineId) && Objects.equals(pipelineName, that.pipelineName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pipelineId, pipelineName);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("pipelineId", pipelineId).add("pipelineName", pipelineName).toString();
  }

  /**
   * A pipeline summary Builder
   */
  public static final class Builder {
    private String pipelineId;
    private String pipelineName;

    private Builder() {}

    /**
     * A pipeline summary builder
     * @return the builder
     */
    public static Builder aPipelineSummary() {
      return new Builder();
    }

    /**
     * With pipelineId builder
     * @param pipelineId
     * @return
     */
    public Builder withPipelineId(String pipelineId) {
      this.pipelineId = pipelineId;
      return this;
    }

    /**
     * With pipeline name builder
     * @param pipelineName
     * @return
     */
    public Builder withPipelineName(String pipelineName) {
      this.pipelineName = pipelineName;
      return this;
    }

    /**
     * But builder.
     * @return
     */
    public Builder but() {
      return aPipelineSummary().withPipelineId(pipelineId).withPipelineName(pipelineName);
    }

    /**
     * Build pipeline summary
     * @return
     */
    public PipelineSummary build() {
      PipelineSummary pipelineSummary = new PipelineSummary();
      pipelineSummary.setPipelineId(pipelineId);
      pipelineSummary.setPipelineName(pipelineName);
      return pipelineSummary;
    }
  }
}
