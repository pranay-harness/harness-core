package software.wings.beans;

import com.google.common.base.MoreObjects;

public class ResponseMessage {
  private String code;
  ;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("code", code)
        .add("errorType", errorType)
        .add("message", message)
        .toString();
  }

  public enum ResponseTypeEnum { INFO, WARN, ERROR }
}
