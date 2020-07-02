package io.harness.common;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class CIExecutionConstants {
  // entry point constants
  public static final String STAGE_ARG_PREFIX = "--stage";
  public static final String LOGPATH_ARG_PREFIX = "--logpath";
  public static final String LITE_ENGINE_COMMAND = "/step-exec/.harness/bin/ci-lite-engine";
  public static final String LOG_PATH = "/.harness/logs/";

  // Image details
  public static final String ADDON_IMAGE_NAME = "harness/ci-addon";
  public static final String ADDON_IMAGE_TAG = "v0.2-alpha";
  public static final String ADDON_CONTAINER_NAME = "addon";
  public static final String ADDON_ARGS =
      "mkdir -p /addon/bin; cp /usr/local/bin/jfrog /addon/bin/jfrog; cp /usr/local/bin/ci-addon /addon/bin/ci-addon; chmod +x /addon/bin/ci-addon; /addon/bin/ci-addon";
  public static final Integer ADDON_PORT = 8001;
  public static final String ADDON_VOLUME = "addon";
  public static final String ADDON_PATH = "/addon";

  public static final String LITE_ENGINE_IMAGE_NAME = "harness/ci-lite-engine";
  public static final String LITE_ENGINE_IMAGE_TAG = "v0.2-alpha";
  public static final String LITE_ENGINE_CONTAINER_NAME = "setup-lite-engine";
  public static final String LITE_ENGINE_ARGS =
      "mkdir -p /step-exec/workspace; mkdir -p /step-exec/.harness/bin; mkdir -p /step-exec/.harness/logs; cp /usr/local/bin/ci-lite-engine-linux-amd64 /step-exec/.harness/bin/ci-lite-engine; chmod +x /step-exec/.harness/bin/ci-lite-engine;";

  public static final String DEFAULT_INTERNAL_IMAGE_CONNECTOR = "CI Harness Images";
  // Deprecated
  public static final List<String> SH_COMMAND = Collections.unmodifiableList(Arrays.asList("sh", "-c", "--"));
  public static final String SETUP_TASK_ARGS = "trap : TERM INT; (while true; do sleep 1000; done) & wait";

  // Container resources
  public static final Integer ADDON_CONTAINER_LIMIT_MEM = 1024;
  public static final Integer ADDON_CONTAINER_REQ_MEM = 1024;
  public static final Integer ADDON_CONTAINER_LIMIT_CPU = 400;
  public static final Integer ADDON_CONTAINER_REQ_CPU = 400;
}
