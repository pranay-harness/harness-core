package software.wings.beans;

import com.google.common.base.MoreObjects;

// TODO: Auto-generated Javadoc

/**
 * The Class ResponseMessage.
 */
public class ResponseMessage {
  private String code;
  private ResponseTypeEnum errorType;
  private String message;

  /**
   * Gets code.
   *
   * @return the code
   */
  public String getCode() {
    return code;
  }

  /**
   * Sets code.
   *
   * @param code the code
   */
  public void setCode(String code) {
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

  /* (non-Javadoc)
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
}
