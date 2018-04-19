package software.wings.exception;

import static java.util.Arrays.asList;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import software.wings.beans.NameValuePair;

import java.util.stream.Collectors;

public class InvalidArgumentsException extends WingsException {
  public InvalidArgumentsException(NameValuePair arg1) {
    super(INVALID_ARGUMENT);
    super.addParam("args",
        asList(arg1).stream().map(pair -> pair.getName() + ": " + pair.getValue()).collect(Collectors.joining("; ")));
  }

  public InvalidArgumentsException(NameValuePair arg1, NameValuePair arg2, Throwable cause) {
    super(INVALID_ARGUMENT, cause);
    super.addParam("args",
        asList(arg1, arg2)
            .stream()
            .map(pair -> pair.getName() + ": " + pair.getValue())
            .collect(Collectors.joining("; ")));
  }
}
