package io.harness.ccm.setup.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class InfraSetUpUtils {
  private static final String EXTERNAL_ID_TEMPLATE = "harness:%s:%s";

  public static String getAwsExternalId(String harnessAccountId, String customerId) {
    return String.format(EXTERNAL_ID_TEMPLATE, harnessAccountId, customerId);
  }
}
