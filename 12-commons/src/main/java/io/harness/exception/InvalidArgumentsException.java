package io.harness.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.stream.Collectors.joining;

import io.harness.eraro.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumSet;
import java.util.stream.Stream;

public class InvalidArgumentsException extends WingsException {
  private static final String ARGS_ARG = "args";

  public InvalidArgumentsException(Pair<String, String> arg1) {
    super(null, null, INVALID_ARGUMENT, Level.ERROR, null, null);
    super.param(ARGS_ARG, Stream.of(arg1).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(Pair<String, String> arg1, Throwable cause, EnumSet<ReportTarget> reportTargets) {
    super(null, cause, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, Stream.of(arg1).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(Pair<String, String> arg1, Throwable cause) {
    super(null, cause, INVALID_ARGUMENT, Level.ERROR, null, null);
    super.param(ARGS_ARG, Stream.of(arg1).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(Pair<String, String> arg1, Pair<String, String> arg2, Throwable cause) {
    super(null, cause, INVALID_ARGUMENT, Level.ERROR, null, null);
    super.param(
        ARGS_ARG, Stream.of(arg1, arg2).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public InvalidArgumentsException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, message);
  }

  public InvalidArgumentsException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, message);
  }
}
