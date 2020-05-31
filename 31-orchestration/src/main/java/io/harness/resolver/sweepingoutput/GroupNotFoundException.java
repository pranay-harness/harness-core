package io.harness.resolver.sweepingoutput;

import static io.harness.eraro.ErrorCode.ENGINE_SWEEPING_OUTPUT_EXCEPTION;
import static java.lang.String.format;

import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class GroupNotFoundException extends WingsException {
  private static final String DETAILS_KEY = "details";

  @SuppressWarnings("squid:CallToDeprecatedMethod")
  public GroupNotFoundException(String groupName) {
    super(groupName, null, ENGINE_SWEEPING_OUTPUT_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, format("Group not found: %s", groupName));
  }
}
