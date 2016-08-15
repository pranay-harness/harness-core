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

  public ContextElement getContextElement() {
    return contextElement;
  }

  public void setContextElement(ContextElement contextElement) {
    this.contextElement = contextElement;
  }

  public Integer getInstancesCount() {
    return instancesCount;
  }

  public void setInstancesCount(Integer instancesCount) {
    this.instancesCount = instancesCount;
  }

  public Long getStartTs() {
    return startTs;
  }

  public void setStartTs(Long startTs) {
    this.startTs = startTs;
  }

  public Long getEndTs() {
    return endTs;
  }

  public void setEndTs(Long endTs) {
    this.endTs = endTs;
  }

  public Integer getTotalTime() {
    if (startTs != null && endTs != null) {
      return Math.toIntExact((endTs - startTs) / 1000);
    }
    return null;
  }

  public void setTotalTime(Integer totalTime) {}

  public Integer getAvgTime() {
    Integer totalTime = getTotalTime();
    if (totalTime != null && instancesCount != null && instancesCount != 0) {
      return totalTime / instancesCount;
    }
    return null;
  }

  public void setAvgTime(Integer avgTime) {}

  public static final class Builder {
    private ContextElement contextElement;
    private Integer instancesCount;
    private Long startTs;
    private Long endTs;

    private Builder() {}

    public static Builder aServiceExecutionSummary() {
      return new Builder();
    }

    public Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    public Builder withInstancesCount(Integer instancesCount) {
      this.instancesCount = instancesCount;
      return this;
    }

    public Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

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
