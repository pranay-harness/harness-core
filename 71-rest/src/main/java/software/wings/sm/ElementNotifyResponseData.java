package software.wings.sm;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.beans.StatusInstanceBreakdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 8/18/16.
 */
@SuppressFBWarnings({"EQ_DOESNT_OVERRIDE_EQUALS"})
public class ElementNotifyResponseData extends ExecutionStatusData {
  private List<ContextElement> contextElements;
  private StatusInstanceBreakdown statusInstanceBreakdown;
  private Long startTs;
  private Long endTs;

  /**
   * Gets context element.
   *
   * @return the context elements
   */
  public List<ContextElement> getContextElements() {
    return contextElements;
  }

  /**
   * Sets context element.
   *
   * @param contextElements the context elements
   */
  public void setContextElements(List<ContextElement> contextElements) {
    this.contextElements = contextElements;
  }

  /**
   * Gets status instance breakdown.
   *
   * @return the status instance breakdown
   */
  public StatusInstanceBreakdown getStatusInstanceBreakdown() {
    return statusInstanceBreakdown;
  }

  /**
   * Sets status instance breakdown.
   *
   * @param statusInstanceBreakdown the status instance breakdown
   */
  public void setStatusInstanceBreakdown(StatusInstanceBreakdown statusInstanceBreakdown) {
    this.statusInstanceBreakdown = statusInstanceBreakdown;
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
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionStatus executionStatus;
    private List<ContextElement> contextElements;
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

    /**
     * With context element element notify response data . builder.
     *
     * @param contextElement the context element
     * @return the element notify response data . builder
     */
    public ElementNotifyResponseData.Builder addContextElement(ContextElement contextElement) {
      if (this.contextElements == null) {
        this.contextElements = new ArrayList<>();
      }
      this.contextElements.add(contextElement);
      return this;
    }

    /**
     * With context element element notify response data . builder.
     *
     * @param contextElements the context element
     * @return the element notify response data . builder
     */
    public ElementNotifyResponseData.Builder addContextElements(List<ContextElement> contextElements) {
      if (this.contextElements == null) {
        this.contextElements = new ArrayList<>();
      }
      this.contextElements.addAll(contextElements);
      return this;
    }

    /**
     * With status instance breakdown element notify response data . builder.
     *
     * @param statusInstanceBreakdown the status instance breakdown
     * @return the element notify response data . builder
     */
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    public ElementNotifyResponseData.Builder withStatusInstanceBreakdown(
        StatusInstanceBreakdown statusInstanceBreakdown) {
      this.statusInstanceBreakdown = statusInstanceBreakdown;
      return this;
    }

    /**
     * With start ts element notify response data . builder.
     *
     * @param startTs the start ts
     * @return the element notify response data . builder
     */
    public ElementNotifyResponseData.Builder withStartTs(Long startTs) {
      this.startTs = startTs;
      return this;
    }

    /**
     * With end ts element notify response data . builder.
     *
     * @param endTs the end ts
     * @return the element notify response data . builder
     */
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
      elementNotifyResponseData.setContextElements(contextElements);
      elementNotifyResponseData.setStartTs(startTs);
      elementNotifyResponseData.setEndTs(endTs);
      return elementNotifyResponseData;
    }
  }
}
