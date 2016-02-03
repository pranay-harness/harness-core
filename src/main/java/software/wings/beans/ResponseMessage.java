package software.wings.beans;

public class ResponseMessage {
  public enum ResponseTypeEnum { INFO, WARN, ERROR }
  ;

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
}
