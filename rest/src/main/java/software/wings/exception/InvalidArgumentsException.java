package software.wings.exception;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import software.wings.beans.NameValuePair;

import java.util.Arrays;
import java.util.stream.Collectors;

public class InvalidArgumentsException extends WingsException {
  public InvalidArgumentsException(Throwable cause, NameValuePair... args) {
    super(INVALID_ARGUMENT, cause);
    super.addParam("args",
        Arrays.stream(args).map(pair -> pair.getName() + ": " + pair.getValue()).collect(Collectors.joining("; ")));
  }
}
