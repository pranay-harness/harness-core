package io.harness.exception;

import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;

import java.util.EnumSet;

public class GraphQLException extends WingsException {
  public GraphQLException(String message, EnumSet<ReportTarget> reportTargets) {
    super(message, null, ErrorCode.GRAPHQL_ERROR, Level.ERROR, reportTargets, null);
    super.getParams().put("message", message);
  }
}
