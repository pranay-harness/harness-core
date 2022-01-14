package io.harness.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class CICommonEndpointConstants {
  public static final String LOG_SERVICE_TOKEN_ENDPOINT = "token";
  public static final String TI_SERVICE_TOKEN_ENDPOINT = "token";
}
