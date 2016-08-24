package software.wings.sm;

import software.wings.beans.StatusInstanceBreakdown;
import software.wings.sm.ExecutionStatus.ExecutionStatusData;

/**
 * Created by rishi on 8/18/16.
 */
public class ElementNotifyResponseData extends ExecutionStatusData {
  private ContextElement contextElement;
  private StatusInstanceBreakdown statusInstanceBreakdown;
  private Long startTs;
  private Long endTs;

  public ContextElement getContextElement() {
    return contextElement;
  }

  public void setContextElement(ContextElement contextElement) {
    this.contextElement = contextElement;
  }

  public StatusInstanceBreakdown getStatusInstanceBreakdown() {
    return statusInstanceBreakdown;
  }

  public void setStatusInstanceBreakdown(StatusInstanceBreakdown statusInstanceBreakdown) {
    this.statusInstanceBreakdown = statusInstanceBreakdown;
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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionStatus executionStatus;
    private ContextElement contextElement;
    private StatusInstanceBreakdown statusInstanceBreakdown;
    private Long startTs;
    private Long endTs;

    private Builder() {}

    /**
     * An execution status data builder.
     *
     * @return the builder
     */
    public static ElementNotifyResponseData.Builder anElementNotifyResponseData() {
      return new ElementNotifyResponseData.Builder();
    }

    /**
     * With execution status builder.
     *
     * @param executionStatus the execution status
     * @return the builder
     */
    public ElementNotifyResponseData.Builder withExecutionStatus(ExecutionStatus executionStatus) {
      this.executionStatus = executionStatus;
      return this;
    }

    public ElementNotifyResponseData.Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
      return this;
    }

    public ElementNotifyResponseData.Builder withStatusInstanceBreakdown(
        StatusInstanceBreakdown statusInstanceBreakdown) {
      this.statusInstanceBreakdown = statusInstanceBreakdown;
      return this;
    }

    public ElementNotifyResponseData.Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    public ElementNotifyResponseData.Builder withEndTs(Long endTs) {
      this.endTs = endTs;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public ElementNotifyResponseData.Builder but() {
      return anElementNotifyResponseData().withExecutionStatus(executionStatus);
    }

    /**
     * Build execution status data.
     *
     * @return the execution status data
     */
    public ElementNotifyResponseData build() {
      ElementNotifyResponseData elementNotifyResponseData = new ElementNotifyResponseData();
      elementNotifyResponseData.setExecutionStatus(executionStatus);
      elementNotifyResponseData.setContextElement(contextElement);
      elementNotifyResponseData.setStartTs(startTs);
      elementNotifyResponseData.setEndTs(endTs);
      return elementNotifyResponseData;
    }
  }
}
