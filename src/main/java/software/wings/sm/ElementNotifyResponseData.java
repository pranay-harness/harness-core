package software.wings.sm;

import software.wings.sm.ExecutionStatus.ExecutionStatusData;

/**
 * Created by rishi on 8/18/16.
 */
public class ElementNotifyResponseData extends ExecutionStatusData {
  private ContextElement contextElement;

  public ContextElement getContextElement() {
    return contextElement;
  }

  public void setContextElement(ContextElement contextElement) {
    this.contextElement = contextElement;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ExecutionStatus executionStatus;
    private ContextElement contextElement;

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
     * With context element builder.
     *
     * @param contextElement the context element
     * @return the builder
     */
    public ElementNotifyResponseData.Builder withContextElement(ContextElement contextElement) {
      this.contextElement = contextElement;
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
      return elementNotifyResponseData;
    }
  }
}
