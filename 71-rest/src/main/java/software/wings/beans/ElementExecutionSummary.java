package software.wings.beans;

import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.InstanceStatusSummary;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by rishi on 8/15/16.
 */
public class ElementExecutionSummary {
  private ContextElement contextElement;
  private List<InstanceStatusSummary> instanceStatusSummaries;
  private Long startTs;
  private Long endTs;
  private ExecutionStatus status;
  private List<InfraMappingSummary> infraMappingSummaries;

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

  public List<InstanceStatusSummary> getInstanceStatusSummaries() {
    return instanceStatusSummaries;
  }

  public void setInstanceStatusSummaries(List<InstanceStatusSummary> instanceStatusSummaries) {
    this.instanceStatusSummaries = instanceStatusSummaries;
  }

  public List<InfraMappingSummary> getInfraMappingSummaries() {
    return infraMappingSummaries;
  }

  public void setInfraMappingSummary(List<InfraMappingSummary> infraMappingSummary) {
    this.infraMappingSummaries = infraMappingSummary;
  }
  /**
   * Gets instances count.
   *
   * @return the instances count
   */
  public Integer getInstancesCount() {
    if (instanceStatusSummaries == null) {
      return 0;
    }
    Set<String> instanceIds = new HashSet<>();
    instanceStatusSummaries.forEach(instanceStatusSummary -> {
      if (instanceStatusSummary.getInstanceElement() != null
          && instanceStatusSummary.getInstanceElement().getUuid() != null) {
        instanceIds.add(instanceStatusSummary.getInstanceElement().getUuid());
      }
    });
    return instanceIds.size();
  }

  /**
   * Sets instances count.
   *
   * @param instancesCount the instances count
   */
  public void setInstancesCount(Integer instancesCount) {}

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

    int instancesCount = getInstancesCount();
    if (totalTime != null && instancesCount != 0) {
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
   * Gets status.
   *
   * @return the status
   */
  public ExecutionStatus getStatus() {
    return status;
  }

  /**
   * Sets status.
   *
   * @param status the status
   */
  public void setStatus(ExecutionStatus status) {
    this.status = status;
  }

  public static final Comparator<? super ElementExecutionSummary> startTsComparator =
      new Comparator<ElementExecutionSummary>() {
        @Override
        public int compare(ElementExecutionSummary o1, ElementExecutionSummary o2) {
          if (o2.getStatus() == ExecutionStatus.QUEUED) {
            return -1;
          } else if (o1.getStatus() == ExecutionStatus.QUEUED) {
            return 1;
          } else if (o2.getStartTs() == null) {
            return -1;
          } else if (o1.getStartTs() == null) {
            return 1;
          } else {
            return o1.getStartTs().compareTo(o2.getStartTs());
          }
        }
      };

  /**
   * The type Element execution summary builder.
   */
  public static final class ElementExecutionSummaryBuilder {
    private ContextElement contextElement;
    private Long startTs;
    private Long endTs;
    private ExecutionStatus status;
    private List<InstanceStatusSummary> instanceStatusSummaries;
    private List<InfraMappingSummary> infraMappingSummaries;

    private ElementExecutionSummaryBuilder() {}

    /**
     * An element execution summary element execution summary builder.
     *
     * @return the element execution summary builder
     */
    public static ElementExecutionSummaryBuilder anElementExecutionSummary() {
      return new ElementExecutionSummaryBuilder();
    }

    /**
     * With context element element execution summary builder.
     *
     * @param contextElement the context element
     * @return the element execution summary builder
     */
    public ElementExecutionSummaryBuilder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    /**
     * With instances count element execution summary builder.
     *
     * @param instanceStatusSummaries the instances summaries
     * @return the element execution summary builder
     */
    public ElementExecutionSummaryBuilder withInstanceStatusSummaries(
        List<InstanceStatusSummary> instanceStatusSummaries) {
      this.instanceStatusSummaries = instanceStatusSummaries;
      return this;
    }

    /**
     * With inframapping Summaries
     * @param infraMappingSummaries
     * @return
     */
    public ElementExecutionSummaryBuilder withInfraMappingSummaries(List<InfraMappingSummary> infraMappingSummaries) {
      this.infraMappingSummaries = infraMappingSummaries;
      return this;
    }

    /**
     * With start ts element execution summary builder.
     *
     * @param startTs the start ts
     * @return the element execution summary builder
     */
    public ElementExecutionSummaryBuilder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts element execution summary builder.
     *
     * @param endTs the end ts
     * @return the element execution summary builder
     */
    public ElementExecutionSummaryBuilder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * With status element execution summary builder.
     *
     * @param status the status
     * @return the element execution summary builder
     */
    public ElementExecutionSummaryBuilder withStatus(ExecutionStatus status) {
      this.status = status;
      return this;
    }

    /**
     * Build element execution summary.
     *
     * @return the element execution summary
     */
    public ElementExecutionSummary build() {
      ElementExecutionSummary elementExecutionSummary = new ElementExecutionSummary();
      elementExecutionSummary.setContextElement(contextElement);
      elementExecutionSummary.setInstanceStatusSummaries(instanceStatusSummaries);
      elementExecutionSummary.setStartTs(startTs);
      elementExecutionSummary.setEndTs(endTs);
      elementExecutionSummary.setStatus(status);
      elementExecutionSummary.setInfraMappingSummary(infraMappingSummaries);
      return elementExecutionSummary;
    }
  }
}
