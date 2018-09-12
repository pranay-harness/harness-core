package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.stream.Collectors.joining;

import io.harness.exception.WingsException;
import software.wings.beans.NameValuePair;

import java.util.stream.Stream;

public class InvalidArgumentsException extends WingsException {
  public InvalidArgumentsException(NameValuePair arg1) {
    super(INVALID_ARGUMENT);
    super.addParam("args", Stream.of(arg1).map(pair -> pair.getName() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(NameValuePair arg1, NameValuePair arg2, Throwable cause) {
    super(INVALID_ARGUMENT, cause);
    super.addParam(
        "args", Stream.of(arg1, arg2).map(pair -> pair.getName() + ": " + pair.getValue()).collect(joining("; ")));
  }
}
