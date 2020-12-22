package io.harness.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CICommonPodConstants {
  public static final String POD_NAME_PREFIX = "harnessci";
  public static final String STEP_EXEC_WORKING_DIR = "harness";
  public static final String CONTAINER_NAME = "build-setup";
  public static final String STEP_EXEC = "step-exec";
  public static final String MOUNT_PATH = "/step-exec";
  public static final String REL_STDOUT_FILE_PATH = "/stdout";
  public static final String REL_STDERR_FILE_PATH = "/stderr";
}
