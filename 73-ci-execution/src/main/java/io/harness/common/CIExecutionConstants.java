package io.harness.common;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class CIExecutionConstants {
  // Addon image
  public static final String ADDON_IMAGE_NAME = "harness/ci-addon";
  public static final String ADDON_IMAGE_TAG = "v1.0-alpha";

  // Lite-engine image
  public static final String LITE_ENGINE_IMAGE_NAME = "harness/ci-lite-engine";
  public static final String LITE_ENGINE_IMAGE_TAG = "v1.0-alpha";

  // Constant for run/plugin step images
  public static final String STEP_COMMAND = "/step-exec/.harness/bin/ci-addon";
  public static final Integer STEP_REQUEST_MEMORY_MIB = 1;
  public static final Integer STEP_REQUEST_MILLI_CPU = 1;
  public static final Integer DEFAULT_STEP_LIMIT_MEMORY_MIB = 200;
  public static final Integer DEFAULT_STEP_LIMIT_MILLI_CPU = 200;
  public static final Integer PORT_STARTING_RANGE = 9000;
  public static final String PLUGIN_ENV_PREFIX = "PLUGIN_";

  // Container constants for setting up addon binary
  public static final String SETUP_ADDON_CONTAINER_NAME = "setup-addon";
  public static final String SETUP_ADDON_ARGS =
      "mkdir -p /step-exec/workspace; mkdir -p /step-exec/.harness/bin; mkdir -p /step-exec/.harness/logs; mkdir -p /step-exec/.harness/tmp; cp /usr/local/bin/ci-addon-linux-amd64 /step-exec/.harness/bin/ci-addon; chmod +x /step-exec/.harness/bin/ci-addon;";

  // Lite engine container constants
  public static final String LITE_ENGINE_CONTAINER_NAME = "lite-engine";
  public static final String LITE_ENGINE_ARGS =
      "mkdir -p /engine/bin; cp /usr/local/bin/jfrog /engine/bin/jfrog; cp /usr/local/bin/ci-lite-engine /engine/bin/ci-lite-engine; chmod +x /engine/bin/ci-lite-engine; /engine/bin/ci-lite-engine";
  public static final String LITE_ENGINE_VOLUME = "engine";
  public static final String LITE_ENGINE_PATH = "/engine";
  public static final String LITE_ENGINE_JFROG_VARIABLE = "JFROG_PATH";
  public static final String LITE_ENGINE_JFROG_PATH = "/engine/bin/jfrog";

  public static final Integer LITE_ENGINE_CONTAINER_MEM = 100;
  public static final Integer LITE_ENGINE_CONTAINER_CPU = 100;

  // entry point constants
  public static final String STAGE_ARG_COMMAND = "stage";
  public static final String INPUT_ARG_PREFIX = "--input";
  public static final String PORT_PREFIX = "--port";
  public static final String TMP_PATH_ARG_PREFIX = "--tmppath";
  public static final String TMP_PATH = "/step-exec/.harness/tmp/";
  public static final String DEBUG_PREFIX = "--debug";

  // Image details
  public static final String ADDON_CONTAINER_NAME = "addon";

  public static final String ACCESS_KEY_MINIO_VARIABLE = "ACCESS_KEY_MINIO";
  public static final String SECRET_KEY_MINIO_VARIABLE = "SECRET_KEY_MINIO";
  public static final String ENDPOINT_MINIO_VARIABLE = "ENDPOINT_MINIO";
  public static final String BUCKET_MINIO_VARIABLE = "BUCKET_MINIO";

  // These are environment variables to be set on the pod for talking to the log service.
  public static final String LOG_SERVICE_TOKEN_VARIABLE = "HARNESS_LOG_SERVICE_TOKEN";
  public static final String LOG_SERVICE_ENDPOINT_VARIABLE = "HARNESS_LOG_SERVICE_ENDPOINT";

  public static final String DELEGATE_SERVICE_TOKEN_VARIABLE = "DELEGATE_SERVICE_TOKEN";
  public static final String DELEGATE_SERVICE_ENDPOINT_VARIABLE = "DELEGATE_SERVICE_ENDPOINT";
  public static final String DELEGATE_SERVICE_ID_VARIABLE = "DELEGATE_SERVICE_ID";
  public static final String DELEGATE_SERVICE_ID_VARIABLE_VALUE = "delegate-grpc-service";
  public static final String DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE = "delegate-service";

  public static final String HARNESS_ACCOUNT_ID_VARIABLE = "HARNESS_ACCOUNT_ID";
  public static final String HARNESS_PROJECT_ID_VARIABLE = "HARNESS_PROJECT_ID";
  public static final String HARNESS_ORG_ID_VARIABLE = "HARNESS_ORG_ID";
  public static final String HARNESS_BUILD_ID_VARIABLE = "HARNESS_BUILD_ID";
  public static final String HARNESS_STAGE_ID_VARIABLE = "HARNESS_STAGE_ID";

  public static final String ENDPOINT_MINIO_VARIABLE_VALUE = "35.224.85.116:9000";
  public static final String BUCKET_MINIO_VARIABLE_VALUE = "test";
  public static final String HOME_VARIABLE = "HOME";

  public static final String DEFAULT_INTERNAL_IMAGE_CONNECTOR = "harnessimage";
  // Deprecated
  public static final List<String> SH_COMMAND = Collections.unmodifiableList(Arrays.asList("sh", "-c", "--"));

  public static final Integer PVC_DEFAULT_STORAGE_SIZE = 25 * 1024;
  public static final String PVC_DEFAULT_STORAGE_CLASS = "faster";
}
