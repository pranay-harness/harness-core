package software.wings.beans;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;

/**
 * Created by rishi on 8/15/16.
 */
public class ElementExecutionSummary {
  private ContextElement contextElement;
  private Integer instancesCount;
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;

  /**
   * Gets context element.
   *
   * @return the context elementElementExecutionSummary
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

  public ExecutionStatus getStatus() {
    return status;
  }

  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public static final class ElementExecutionSummaryBuilder {
    private ContextElement contextElement;
    private Integer instancesCount;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;

    private ElementExecutionSummaryBuilder() {}

    public static ElementExecutionSummaryBuilder anElementExecutionSummary() {
      return new ElementExecutionSummaryBuilder();
    }

    public ElementExecutionSummaryBuilder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    public ElementExecutionSummaryBuilder withInstancesCount(Integer instancesCount) {
      this.instancesCount = instancesCount;
      return this;
    }

    public ElementExecutionSummaryBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public ElementExecutionSummaryBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    public ElementExecutionSummaryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    public ElementExecutionSummary build() {
      ElementExecutionSummary elementExecutionSummary = new ElementExecutionSummary();
      elementExecutionSummary.setContextElement(contextElement);
      elementExecutionSummary.setInstancesCount(instancesCount);
      elementExecutionSummary.setStartTs(startTs);
      elementExecutionSummary.setEndTs(endTs);
      elementExecutionSummary.setStatus(status);
      return elementExecutionSummary;
    }
  }
}
