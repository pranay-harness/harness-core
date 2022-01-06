/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.exception;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;

import static java.util.stream.Collectors.joining;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eraro.Level;

import java.util.EnumSet;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
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

  public InvalidArgumentsException(String message) {
    super(message, null, INVALID_ARGUMENT, Level.ERROR, null, null);
    super.param(ARGS_ARG, message);
  }

  public InvalidArgumentsException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, message);
  }

  public InvalidArgumentsException(String message, Throwable t, EnumSet<ReportTarget> reportTargets) {
    super(message, t, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, message);
  }

  public InvalidArgumentsException(String message, EnumSet<ReportTarget> reportTargets, Throwable cause) {
    super(message, cause, INVALID_ARGUMENT, Level.ERROR, reportTargets, null);
    super.param(ARGS_ARG, message);
  }

  public InvalidArgumentsException(Pair<String, String> arg1, String helpMessage) {
    super(null, null, INVALID_ARGUMENT, Level.ERROR, null, null);
    String invalidArgument = Stream.of(arg1).map(pair -> pair.getKey() + ": " + pair.getValue()).collect(joining("; "));
    String finalMessage = invalidArgument + ". " + helpMessage;
    super.param(ARGS_ARG, finalMessage);
  }
}
