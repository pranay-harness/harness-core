package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.stream.Collectors.joining;

import org.apache.commons.lang3.tuple.Pair;

import java.util.stream.Stream;

public class InvalidArgumentsException extends WingsException {
  public InvalidArgumentsException(Pair<String, String> arg1) {
    super(INVALID_ARGUMENT);
    super.addParam("args", Stream.of(arg1).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(Pair<String, String> arg1, Pair<String, String> arg2, Throwable cause) {
    super(INVALID_ARGUMENT, cause);
    super.addParam(
        "args", Stream.of(arg1, arg2).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }
}
