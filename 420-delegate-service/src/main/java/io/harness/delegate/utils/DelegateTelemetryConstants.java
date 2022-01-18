package io.harness.delegate.utils;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DEL)
public class DelegateTelemetryConstants {
  public static final String DELEGATE_CREATED_EVENT = "Delegate Created";
  public static final String DELEGATE_REGISTERED_EVENT = "Delegate Registered";
  public static final String COUNT_OF_CONNECTED_DELEGATES = "Count of connected delegates";
  public static final String COUNT_OF_REGISTERED_DELEGATES = "Count of registered delegates";
}
