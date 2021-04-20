package io.harness.pms.execution.utils;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.contracts.execution.failure.FailureType;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

@OwnedBy(HarnessTeam.PIPELINE)
public class EngineExceptionUtils {
  public static EnumSet<FailureType> getOrchestrationFailureTypes(Throwable throwable) {
    EnumSet<io.harness.exception.FailureType> hFailureTypes = ExceptionUtils.getFailureTypes(throwable);
    return transformToOrchestrationFailureTypes(hFailureTypes);
  }

  public static EnumSet<FailureType> transformToOrchestrationFailureTypes(
      Collection<io.harness.exception.FailureType> hFailureTypes) {
    if (hFailureTypes == null) {
      hFailureTypes = Collections.emptyList();
    }
    EnumSet<FailureType> orchestrationFailureTypes = EnumSet.noneOf(FailureType.class);
    if (hFailureTypes.isEmpty()) {
      return orchestrationFailureTypes;
    }

    for (io.harness.exception.FailureType hFailureType : hFailureTypes) {
      orchestrationFailureTypes.add(mapToOrchestrationFailureType(hFailureType));
    }
    return orchestrationFailureTypes;
  }

  public static EnumSet<io.harness.exception.FailureType> transformToWingsFailureTypes(
      Collection<FailureType> oFailureTypes) {
    EnumSet<io.harness.exception.FailureType> wingsFailureType = EnumSet.noneOf(io.harness.exception.FailureType.class);
    if (oFailureTypes.isEmpty()) {
      return wingsFailureType;
    }

    for (FailureType oFailureType : oFailureTypes) {
      wingsFailureType.add(mapToWingsFailureType(oFailureType));
    }
    return wingsFailureType;
  }

  @VisibleForTesting
  static io.harness.exception.FailureType mapToWingsFailureType(FailureType oFailureType) {
    switch (oFailureType) {
      case TIMEOUT_FAILURE:
        return io.harness.exception.FailureType.EXPIRED;
      case UNRECOGNIZED:
      case UNKNOWN_FAILURE:
      case SKIPPING_FAILURE:
      case APPLICATION_FAILURE:
        return io.harness.exception.FailureType.APPLICATION_ERROR;
      case CONNECTIVITY_FAILURE:
        return io.harness.exception.FailureType.CONNECTIVITY;
      case VERIFICATION_FAILURE:
        return io.harness.exception.FailureType.VERIFICATION_FAILURE;
      case AUTHORIZATION_FAILURE:
        return io.harness.exception.FailureType.AUTHORIZATION_ERROR;
      case AUTHENTICATION_FAILURE:
        return io.harness.exception.FailureType.AUTHENTICATION;
      case DELEGATE_PROVISIONING_FAILURE:
        return io.harness.exception.FailureType.DELEGATE_PROVISIONING;
      default:
        throw new InvalidRequestException("No failure mapped to " + oFailureType.name());
    }
  }

  private static FailureType mapToOrchestrationFailureType(io.harness.exception.FailureType hFailureType) {
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
      case EXPIRED:
        return FailureType.TIMEOUT_FAILURE;
      default:
        return FailureType.UNKNOWN_FAILURE;
    }
  }
}
