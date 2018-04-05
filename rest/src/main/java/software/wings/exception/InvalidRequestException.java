package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;

public class InvalidRequestException extends WingsException {
  public InvalidRequestException(String message) {
    super(INVALID_REQUEST);
    super.addParam("message", message);
  }
}
