/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.openshift;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.TimeUnit;

@OwnedBy(CDP)
public final class OpenShiftConstants {
  private OpenShiftConstants() {}

  public static final String TEMPLATE_FILE_PATH = "${TEMPLATE_FILE_PATH}";
  public static final String OC_BINARY_PATH = "${OC_BINARY_PATH}";
  public static final String PROCESS_COMMAND =
      OC_BINARY_PATH + " process -f " + TEMPLATE_FILE_PATH + " --local -o yaml";
  public static final long COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
}
