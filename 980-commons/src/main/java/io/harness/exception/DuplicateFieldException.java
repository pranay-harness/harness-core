package io.harness.exception;

import static io.harness.eraro.ErrorCode.DUPLICATE_FIELD;
import static java.util.stream.Collectors.joining;

import io.harness.eraro.Level;
import org.apache.commons.lang3.tuple.Pair;

import java.util.EnumSet;
import java.util.stream.Stream;

public class DuplicateFieldException extends WingsException {
  private static final String MESSAGE_ARG = "message";

  public DuplicateFieldException(Pair<String, String> args) {
    super(null, null, DUPLICATE_FIELD, Level.ERROR, null, null);
    super.param("args", Stream.of(args).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; ")));
  }

  public DuplicateFieldException(String message) {
    super(message, null, DUPLICATE_FIELD, Level.ERROR, null, null);
    super.param(MESSAGE_ARG, message);
  }

  public DuplicateFieldException(String message, EnumSet<ReportTarget> reportTarget, Throwable e) {
    super(message, e, DUPLICATE_FIELD, Level.ERROR, reportTarget, null);
    super.param(MESSAGE_ARG, message);
  }
}
