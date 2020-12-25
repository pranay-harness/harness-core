package io.harness.common;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CIExecutionConstants {
  // Pipeline constants
  public static final String CI_PIPELINE_CONFIG = "CI_PIPELINE_CONFIG";

  // Addon image
  public static final String ADDON_IMAGE_NAME = "harness/ci-addon";

  // Lite-engine image
  public static final String LITE_ENGINE_IMAGE_NAME = "harness/ci-lite-engine";

  // Constants for implicit git clone step
  public static final String GIT_CLONE_STEP_ID = "harness-git-clone";
  public static final String GIT_CLONE_STEP_NAME = "Git clone";
  public static final String GIT_CLONE_IMAGE = "drone/git";
  public static final Integer GIT_CLONE_DEPTH = 50;
  public static final Integer GIT_CLONE_MANUAL_DEPTH = 1;
  public static final String GIT_CLONE_DEPTH_ATTRIBUTE = "depth";
  public static final String GIT_URL_SUFFIX = ".git";
  public static final String GIT_SSH_URL_PREFIX = "git@";
  public static final String PATH_SEPARATOR = "/";

  // Constant for run/plugin step images
  public static final String STEP_COMMAND = "/step-exec/.harness/bin/ci-addon";
  public static final Integer STEP_REQUEST_MEMORY_MIB = 1;
  public static final Integer STEP_REQUEST_MILLI_CPU = 1;
  public static final Integer PORT_STARTING_RANGE = 9000;
  public static final String PLUGIN_ENV_PREFIX = "PLUGIN_";

  public static final String LOCALHOST_IP = "127.0.0.1";
  public static final String SERVICE_PREFIX = "service-";
  public static final String STEP_PREFIX = "step-";
  public static final String SHARED_VOLUME_PREFIX = "shared-";

  // Container constants for setting up addon binary
  public static final String SETUP_ADDON_CONTAINER_NAME = "setup-addon";
  public static final String SETUP_ADDON_ARGS =
      "mkdir -p ${HARNESS_WORKSPACE}; mkdir -p /step-exec/.harness/bin; mkdir -p /step-exec/.harness/logs; mkdir -p /step-exec/.harness/tmp; cp /usr/local/bin/ci-addon-linux-amd64 /step-exec/.harness/bin/ci-addon; chmod +x /step-exec/.harness/bin/ci-addon;";

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
  public static final String SERVICE_ARG_COMMAND = "service";
  public static final String IMAGE_PREFIX = "--image";
  public static final String ID_PREFIX = "--id";
  public static final String ENTRYPOINT_PREFIX = "--entrypoint";
  public static final String ARGS_PREFIX = "--args";
  public static final String GRPC_SERVICE_PORT_PREFIX = "--svc_ports";
  public static final String DEBUG_PREFIX = "--debug";

  public static final String ACCESS_KEY_MINIO_VARIABLE = "ACCESS_KEY_MINIO";
  public static final String SECRET_KEY_MINIO_VARIABLE = "SECRET_KEY_MINIO";
  public static final String ENDPOINT_MINIO_VARIABLE = "ENDPOINT_MINIO";
  public static final String BUCKET_MINIO_VARIABLE = "BUCKET_MINIO";

  // These are environment variables to be set on the pod for talking to the log service.
  public static final String LOG_SERVICE_TOKEN_VARIABLE = "HARNESS_LOG_SERVICE_TOKEN";
  public static final String LOG_SERVICE_ENDPOINT_VARIABLE = "HARNESS_LOG_SERVICE_ENDPOINT";

  // These are environment variables to be set on the pod for talking to the TI service.
  public static final String TI_SERVICE_ENDPOINT_VARIABLE = "HARNESS_TI_SERVICE_ENDPOINT";

  public static final String DELEGATE_SERVICE_TOKEN_VARIABLE = "DELEGATE_SERVICE_TOKEN";
  public static final String DELEGATE_SERVICE_ENDPOINT_VARIABLE = "DELEGATE_SERVICE_ENDPOINT";
  public static final String DELEGATE_SERVICE_ID_VARIABLE = "DELEGATE_SERVICE_ID";
  public static final String DELEGATE_SERVICE_ID_VARIABLE_VALUE = "delegate-grpc-service";
  public static final String DELEGATE_SERVICE_ENDPOINT_VARIABLE_VALUE = "delegate-service:8080";

  public static final String HARNESS_ACCOUNT_ID_VARIABLE = "HARNESS_ACCOUNT_ID";
  public static final String HARNESS_PROJECT_ID_VARIABLE = "HARNESS_PROJECT_ID";
  public static final String HARNESS_ORG_ID_VARIABLE = "HARNESS_ORG_ID";
  public static final String HARNESS_BUILD_ID_VARIABLE = "HARNESS_BUILD_ID";
  public static final String HARNESS_STAGE_ID_VARIABLE = "HARNESS_STAGE_ID";

  public static final String ENDPOINT_MINIO_VARIABLE_VALUE = "35.224.85.116:9000";
  public static final String BUCKET_MINIO_VARIABLE_VALUE = "test";
  public static final String HARNESS_WORKSPACE = "HARNESS_WORKSPACE";

  // Deprecated
  public static final List<String> SH_COMMAND = Collections.unmodifiableList(Arrays.asList("sh", "-c", "--"));

  public static final String IMAGE_PATH_SPLIT_REGEX = ":";
  public static final String PVC_DEFAULT_STORAGE_CLASS = "faster";
}
