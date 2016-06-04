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

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public ResponseTypeEnum getErrorType() {
    return errorType;
  }

  public void setErrorType(ResponseTypeEnum errorType) {
    this.errorType = errorType;
  }

  public String getMessage() {
    return message;
  }

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
  public enum ResponseTypeEnum { INFO, WARN, ERROR }
}
