/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.common;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(HarnessTeam.CI)
public class CICommonPodConstants {
  public static final String POD_NAME_PREFIX = "harnessci";
  public static final String CONTAINER_NAME = "build-setup";
  public static final String REL_STDOUT_FILE_PATH = "/stdout";
  public static final String REL_STDERR_FILE_PATH = "/stderr";
}
