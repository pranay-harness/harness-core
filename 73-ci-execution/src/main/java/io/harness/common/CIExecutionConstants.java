package io.harness.common;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class CIExecutionConstants {
  // entry point constants
  public static final String LITE_ENGINE_COMMAND = "/step-exec/.harness/bin/ci-lite-engine";
  public static final String STAGE_ARG_COMMAND = "stage";
  public static final String INPUT_ARG_PREFIX = "--input";
  public static final String PORTS_PREFIX = "--ports";
  public static final String PORT_PREFIX = "--port";
  public static final String SERVER_PREFIX = "server";
  public static final String LOG_PATH_ARG_PREFIX = "--logpath";
  public static final String LOG_PATH = "/step-exec/.harness/logs/";
  public static final String TMP_PATH_ARG_PREFIX = "--tmppath";
  public static final String TMP_PATH = "/step-exec/.harness/tmp/";
  public static final String DEBUG_PREFIX = "--debug";

  public static final String LITE_ENGINE_STEP_COMMAND_FORMAT =
      "/step-exec/.harness/bin/ci-lite-engine step --input %s --logpath %s --tmppath %s";

  // Image details
  public static final String ADDON_IMAGE_NAME = "harness/ci-addon";
  public static final String ADDON_IMAGE_TAG = "valpha-0.91";
  public static final String ADDON_CONTAINER_NAME = "addon";
  public static final String ADDON_ARGS =
      "mkdir -p /addon/bin; cp /usr/local/bin/jfrog /addon/bin/jfrog; cp /usr/local/bin/ci-addon /addon/bin/ci-addon; chmod +x /addon/bin/ci-addon; /addon/bin/ci-addon";
  public static final Integer ADDON_PORT = 8001;
  public static final String ADDON_VOLUME = "addon";
  public static final String ADDON_PATH = "/addon";
  public static final String ADDON_JFROG_VARIABLE = "JFROG_PATH";
  public static final String ADDON_JFROG_PATH = "/addon/bin/jfrog";

  public static final String ACCESS_KEY_MINIO_VARIABLE = "ACCESS_KEY_MINIO";
  public static final String SECRET_KEY_MINIO_VARIABLE = "SECRET_KEY_MINIO";
  public static final String ENDPOINT_MINIO_VARIABLE = "ENDPOINT_MINIO";
  public static final String BUCKET_MINIO_VARIABLE = "BUCKET_MINIO";

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
  public static final String HARNESS_STEP_ID_VARIABLE = "HARNESS_STEP_ID";

  public static final String ENDPOINT_MINIO_VARIABLE_VALUE = "35.224.85.116:9000";
  public static final String BUCKET_MINIO_VARIABLE_VALUE = "test";

  public static final String LOG_SERVICE_ENDPOINT_VARIABLE = "LOG_SERVICE_ENDPOINT";
  public static final String LOG_SERVICE_ENDPOINT_VARIABLE_VALUE = "http://34.122.43.109:80";

  public static final String LITE_ENGINE_IMAGE_NAME = "harness/ci-lite-engine";

  public static final String LITE_ENGINE_IMAGE_TAG = "valpha-0.91";
  public static final String LITE_ENGINE_CONTAINER_NAME = "setup-lite-engine";
  public static final String LITE_ENGINE_ARGS =
      "mkdir -p /step-exec/workspace; mkdir -p /step-exec/.harness/bin; mkdir -p /step-exec/.harness/logs; mkdir -p /step-exec/.harness/tmp; cp /usr/local/bin/ci-lite-engine-linux-amd64 /step-exec/.harness/bin/ci-lite-engine; chmod +x /step-exec/.harness/bin/ci-lite-engine;";

  public static final String DEFAULT_INTERNAL_IMAGE_CONNECTOR = "harnessimage";
  // Deprecated
  public static final List<String> SH_COMMAND = Collections.unmodifiableList(Arrays.asList("sh", "-c", "--"));
  public static final String SETUP_TASK_ARGS = "trap : TERM INT; (while true; do sleep 1000; done) & wait";

  // Container resources
  public static final Integer ADDON_CONTAINER_LIMIT_MEM = 1024;
  public static final Integer ADDON_CONTAINER_REQ_MEM = 1024;
  public static final Integer ADDON_CONTAINER_LIMIT_CPU = 400;
  public static final Integer ADDON_CONTAINER_REQ_CPU = 400;

  public static final Integer PVC_DEFAULT_STORAGE_SIZE = 25 * 1024;
  public static final String PVC_DEFAULT_STORAGE_CLASS = "faster";
  public static final Integer PORT_STARTING_RANGE = 9000;
}
