package software.wings.beans;

import software.wings.sm.ContextElement;

/**
 * Created by rishi on 8/15/16.
 */
public class ElementExecutionSummary {
  private ContextElement contextElement;
  private Integer instancesCount;
  private Long startTs;
  private Long endTs;

  /**
   * Gets context element.
   *
   * @return the context element
   */
  public ContextElement getContextElement() {
    return contextElement;
  }

  /**
   * Sets context element.
   *
   * @param contextElement the context element
   */
  public void setContextElement(ContextElement contextElement) {
    this.contextElement = contextElement;
  }

  /**
   * Gets instances count.
   *
   * @return the instances count
   */
  public Integer getInstancesCount() {
    return instancesCount;
  }

  /**
   * Sets instances count.
   *
   * @param instancesCount the instances count
   */
  public void setInstancesCount(Integer instancesCount) {
    this.instancesCount = instancesCount;
  }

  /**
   * Gets start ts.
   *
   * @return the start ts
   */
  public Long getStartTs() {
    return startTs;
  }

  /**
   * Sets start ts.
   *
   * @param startTs the start ts
   */
  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  /**
   * Gets end ts.
   *
   * @return the end ts
   */
  public Long getEndTs() {
    return endTs;
  }

  /**
   * Sets end ts.
   *
   * @param endTs the end ts
   */
  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  /**
   * Gets total time.
   *
   * @return the total time
   */
  public Integer getTotalTime() {
    if (startTs != null && endTs != null) {
      return Math.toIntExact((endTs - startTs) / 1000);
    }
    return null;
  }

  /**
   * Sets total time.
   *
   * @param totalTime the total time
   */
  public void setTotalTime(Integer totalTime) {}

  /**
   * Gets avg time.
   *
   * @return the avg time
   */
  public Integer getAvgTime() {
    Integer totalTime = getTotalTime();
    if (totalTime != null && instancesCount != null && instancesCount != 0) {
      return totalTime / instancesCount;
    }
    return null;
  }

  /**
   * Sets avg time.
   *
   * @param avgTime the avg time
   */
  public void setAvgTime(Integer avgTime) {}

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ContextElement contextElement;
    private Integer instancesCount;
    private Long startTs;
    private Long endTs;

    private Builder() {}

    /**
     * A service execution summary builder.
     *
     * @return the builder
     */
    public static Builder aServiceExecutionSummary() {
      return new Builder();
    }

    /**
     * With context element builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    /**
     * With instances count builder.
     *
     * @param instancesCount the instances count
     * @return the builder
     */
    public Builder withInstancesCount(Integer instancesCount) {
      this.instancesCount = instancesCount;
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
     * Build element execution summary.
     *
     * @return the element execution summary
     */
    public ElementExecutionSummary build() {
      ElementExecutionSummary serviceExecutionSummary = new ElementExecutionSummary();
      serviceExecutionSummary.setContextElement(contextElement);
      serviceExecutionSummary.setInstancesCount(instancesCount);
      serviceExecutionSummary.setStartTs(startTs);
      serviceExecutionSummary.setEndTs(endTs);
      return serviceExecutionSummary;
    }
  }
}
