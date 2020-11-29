package io.harness.engine;

import io.harness.exception.ExceptionUtils;
import io.harness.pms.execution.failure.FailureType;

import java.util.EnumSet;

public class EngineExceptionUtils {
  public static EnumSet<FailureType> getOrchestrationFailureTypes(Throwable throwable) {
    EnumSet<io.harness.exception.FailureType> hFailureTypes = ExceptionUtils.getFailureTypes(throwable);
    return transformFailureTypes(hFailureTypes);
  }

  public static EnumSet<FailureType> transformFailureTypes(EnumSet<io.harness.exception.FailureType> hFailureTypes) {
    EnumSet<io.harness.pms.execution.failure.FailureType> orchestrationFailureTypes =
        EnumSet.noneOf(io.harness.pms.execution.failure.FailureType.class);
    if (hFailureTypes.isEmpty()) {
      return orchestrationFailureTypes;
    }

    for (io.harness.exception.FailureType hFailureType : hFailureTypes) {
      orchestrationFailureTypes.add(mapFailureType(hFailureType));
    }
    return orchestrationFailureTypes;
  }

  private static io.harness.pms.execution.failure.FailureType mapFailureType(
      io.harness.exception.FailureType hFailureType) {
    switch (hFailureType) {
      case DELEGATE_PROVISIONING:
        return FailureType.DELEGATE_PROVISIONING_FAILURE;
      case CONNECTIVITY:
        return FailureType.CONNECTIVITY_FAILURE;
      case AUTHENTICATION:
        return FailureType.AUTHENTICATION_FAILURE;
      case VERIFICATION_FAILURE:
        return FailureType.VERIFICATION_FAILURE;
      case APPLICATION_ERROR:
        return FailureType.APPLICATION_FAILURE;
      case AUTHORIZATION_ERROR:
        return FailureType.AUTHORIZATION_FAILURE;
      default:
        return FailureType.UNKNOWN_FAILURE;
    }
  }
}
