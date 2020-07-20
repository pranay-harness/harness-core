package io.harness.mongo;

import static io.harness.eraro.ErrorCode.REGISTRY_EXCEPTION;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;

public class OrchestrationAliasRegistryException extends WingsException {
  private static final String DETAILS_KEY = "details";

  public OrchestrationAliasRegistryException(String messagePrefix, String errorLiteral) {
    super(HarnessStringUtils.join(" ", messagePrefix, errorLiteral), null, REGISTRY_EXCEPTION, Level.ERROR, null, null);
    super.param(DETAILS_KEY, HarnessStringUtils.join(" ", messagePrefix, errorLiteral));
  }
}
