package software.wings.beans;

import com.google.common.base.MoreObjects;

// TODO: Auto-generated Javadoc

/**
 * The Class ResponseMessage.
 */
public class ResponseMessage {
  private ErrorConstants code;
  private ResponseTypeEnum errorType;
  private String message;

  /**
   * Gets code.
   *
   * @return the code
   */
  public ErrorConstants getCode() {
    return code;
  }

  /**
   * Sets code.
   *
   * @param code the code
   */
  public void setCode(ErrorConstants code) {
    this.code = code;
  }

  /**
   * Gets error type.
   *
   * @return the error type
   */
  public ResponseTypeEnum getErrorType() {
    return errorType;
  }

  /**
   * Sets error type.
   *
   * @param errorType the error type
   */
  public void setErrorType(ResponseTypeEnum errorType) {
    this.errorType = errorType;
  }

  /**
   * Gets message.
   *
   * @return the message
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets message.
   *
   * @param message the message
   */
  public void setMessage(String message) {
    this.message = message;
  }

  /** {@inheritDoc} */ /* (non-Javadoc)
                        * @see java.lang.Object#toString()
                        */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("code", code)
        .add("errorType", errorType)
        .add("message", message)
        .toString();
  }

  /**
   * The Enum ResponseTypeEnum.
   */
  public enum ResponseTypeEnum {
    /**
     * Info response type enum.
     */
    INFO, /**
           * Warn response type enum.
           */
    WARN, /**
           * Error response type enum.
           */
    ERROR
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private ErrorConstants code;
    private ResponseTypeEnum errorType;
    private String message;

    /** Do not instantiate Builder. */
    private Builder() {}

    public static Builder aResponseMessage() {
      return new Builder();
    }

    public Builder withCode(ErrorConstants code) {
      this.code = code;
      return this;
    }

    public Builder withErrorType(ResponseTypeEnum errorType) {
      this.errorType = errorType;
      return this;
    }

    public Builder withMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder but() {
      return aResponseMessage().withCode(code).withErrorType(errorType).withMessage(message);
    }

    public ResponseMessage build() {
      ResponseMessage responseMessage = new ResponseMessage();
      responseMessage.setCode(code);
      responseMessage.setErrorType(errorType);
      responseMessage.setMessage(message);
      return responseMessage;
    }
  }
}
