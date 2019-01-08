package software.wings.utils;

import io.harness.limits.Action;
import io.harness.limits.ActionType;
import io.harness.limits.checker.StaticLimitCheckerWithDecrement;
import io.harness.limits.lib.StaticLimit;
import lombok.AllArgsConstructor;
import lombok.Value;

/**
 * Created by anubhaw on 5/26/16.
 */
public interface WingsTestConstants {
  String APP_ID = "APP_ID";

  String TARGET_APP_ID = "TARGET_APP_ID";

  String APP_NAME = "APP_NAME";

  String SERVICE_ID = "SERVICE_ID";

  String SERVICE_TEMPLATE_ID = "SERVICE_TEMPLATE_ID";

  String PROVISIONER_ID = "PROVISIONER_ID";

  String SERVICE_COMMAND_ID = "SERVICE_COMMAND_ID";

  String TARGET_SERVICE_ID = "TARGET_SERVICE_ID";

  String SERVICE_ID_CHANGED = "SERVICE_ID_CHANGED";

  String SERVICE_NAME = "SERVICE_NAME";

  String INFRA_NAME = "INFRA_NAME";

  String PROVISIONER_NAME = "PROVISIONER_NAME";

  String ENV_ID = "ENV_ID";

  String ENV_ID_CHANGED = "ENV_ID_CHANGED";

  String ENV_NAME = "ENV_NAME";

  String ENV_DESCRIPTION = "ENV_DESCRIPTION";

  String ARTIFACT_ID = "ARTIFACT_ID";

  String ARTIFACT_NAME = "ARTIFACT_DISPLAY_NAME";

  String ARTIFACT_STREAM_ID = "ARTIFACT_STREAM_ID";

  String ARTIFACT_STREAM_NAME = "ARTIFACT_STREAM_NAME";

  String ARTIFACT_SOURCE_NAME = "ARTIFACT_SOURCE_NAME";

  String BUILD_JOB_NAME = "BUILD_JOB_NAME";

  String BUILD_NO = "BUILD_NO";

  String ARTIFACT_GROUP_ID = "ARTIFACT_GROUP_ID";

  String STATE_EXECUTION_ID = "STATE_EXECUTION_ID";

  String SERVICE_INSTANCE_ID = "SERVICE_INSTANCE_ID";

  String HOST_ID = "HOST_ID";

  String HOST_NAME = "HOST_NAME";

  String TEMPLATE_ID = "TEMPLATE_ID";

  String TEMPLATE_NAME = "TEMPLATE_NAME";

  String TEMPLATE_DESCRIPTION = "TEMPLATE_DESCRIPTION";

  String FILE_NAME = "FILE_NAME";

  String FILE_PATH = "FILE_PATH";

  String FILE_ID = "FILE_ID";

  String SSH_USER_NAME = "SSH_USER_NAME";

  char[] SSH_USER_PASSWORD = "SSH_USER_PASSWORD".toCharArray();

  char[] SSH_KEY = "SSH_KEY".toCharArray();

  String DELEGATE_ID = "DELEGATE_ID";

  String ACTIVITY_ID = "ACTIVITY_ID";

  String STATE_MACHINE_ID = "STATE_MACHINE_ID";

  String WORKFLOW_ID = "WORKFLOW_ID";

  String WORKFLOW_NAME = "WORKFLOW_NAME";

  Integer DEFAULT_VERSION = 1000;

  String PIPELINE_ID = "PIPELINE_ID";

  String PIPELINE_NAME = "PIPELINE_NAME";

  String PIPELINE_EXECUTION_ID = "PIPELINE_EXECUTION_ID";

  String WORKFLOW_EXECUTION_ID = "WORKFLOW_EXECUTION_ID";

  String PIPELINE_WORKFLOW_EXECUTION_ID = "PIPELINE_WORKFLOW_EXECUTION_ID";

  String JENKINS_URL = "JENKINS_URL";

  String JOB_NAME = "JOB_NAME";

  String ARTIFACT_PATH = "ARTIFACT_PATH";

  String ARTIFACTS = "ARTIFACTS";

  String USER_NAME = "USER_NAME";

  String USER_EMAIL = "user@wings.software";

  String INVALID_USER_EMAIL = "user@@non-existent.com";

  String COMPANY_NAME = "COMPANY_NAME";

  String ACCOUNT_NAME = "ACCOUNT_NAME";

  char[] PASSWORD = "PASSWORD".toCharArray();

  char[] USER_PASSWORD = "USER_PASSWORD".toCharArray();

  String COMMAND_NAME = "COMMAND_NAME";

  String COMMAND_UNIT_NAME = "COMMAND_UNIT_NAME";

  String COMMAND_UNIT_TYPE = "COMMAND_UNIT_TYPE";

  String SETTING_ID = "SETTING_ID";

  String RUNTIME_PATH = "RUNTIME_PATH";

  String LOG_ID = "LOG_ID";

  String USER_ID = "USER_ID";

  String USER_INVITE_ID = "USER_INVITE_ID";

  String PORTAL_URL = "PORTAL_URL";

  String VERIFICATION_PATH = "VERIFICATION_PATH";

  String FREEMIUM_ENV_PATH = "GRATIS";

  String NOTIFICATION_ID = "NOTIFICATION_ID";

  String NOTIFICATION_GROUP_ID = "NOTIFICATION_GROUP_ID";

  String ROLE_NAME = "ROLE_NAME";

  String ROLE_ID = "ROLE_ID";

  String SERVICE_VARIABLE_ID = "SERVICE_VARIABLE_ID";

  String SERVICE_VARIABLE_NAME = "SERVICE_VARIABLE_NAME";

  String HOST_CONN_ATTR_ID = "HOST_CONN_ATTR_ID";

  String BASTION_CONN_ATTR_ID = "BASTION_CONN_ATTR_ID";

  String HOST_CONN_ATTR_KEY_ID = "HOST_CONN_ATTR_KEY_ID";

  String ACCOUNT_ID = "ACCOUNT_ID";

  String ACCOUNT_KEY = "ACCOUNT_KEY_ACCOUNT_KEY_ACCOUNT_"; // Account key must be 32 characters

  String ACCESS_KEY = "ACCESS_KEY";

  char[] SECRET_KEY = "SECRET_KEY".toCharArray();

  String ASSERTION = "ASSERTION";

  String NAMESPACE = "AWS/EC2";

  String METRIC_NAME = "CPUUtilization";

  String METRIC_DIMENSION = "METRIC_DIMENSION";

  String CLUSTER_NAME = "CLUSTER_NAME";

  String SERVICE_DEFINITION = "SERVICE_DEFINITION";

  String LAUNCHER_TEMPLATE_NAME = "LAUNCHER_TEMPLATE_NAME";

  String AUTO_SCALING_GROUP_NAME = "AUTO_SCALING_GROUP_NAME";

  String INFRA_MAPPING_ID = "INFRA_MAPPING_ID";

  String INFRA_MAPPING_ID_CHANGED = "INFRA_MAPPING_ID_CHANGED";

  String COMPUTE_PROVIDER_ID = "COMPUTE_PROVIDER_ID";

  String COMPUTE_PROVIDER_ID_CHANGED = "COMPUTE_PROVIDER_ID_CHANGED";

  String STATE_NAME = "STATE_NAME";

  String TASK_FAMILY = "TASK_FAMILY";

  Integer TASK_REVISION = 100;

  String ECS_SERVICE_NAME = "ECS_SERVICE_NAME";

  String PCF_SERVICE_NAME = "PCF_SERVICE_NAME";

  String PHASE_STEP = "PHASE_STEP";

  String PHASE_ID = "PHASE_ID";

  String TRIGGER_ID = "TRIGGER_ID";

  String TRIGGER_NAME = "TRIGGER_NAME";

  String TRIGGER_DESCRIPTION = "TRIGGER_DESCRIPTION";

  String ARTIFACT_FILTER = "ARTIFACT_FILTER";

  String USER_GROUP_ID = "USER_GROUP_ID";

  int INTEGER_DEFAULT_VALUE = Integer.MAX_VALUE;

  long LONG_DEFAULT_VALUE = Long.MAX_VALUE;

  float FLOAT_DEFAULT_VALUE = Float.MAX_VALUE;

  double DOUBLE_DEFAULT_VALUE = Double.MAX_VALUE;

  String INVALID_NAME = "aba$$%55";

  String HARNESS_NEXUS = "Harness Nexus";
  String HARNESS_JENKINS = "Harness Jenkins";
  String HARNESS_NEXUS_THREE = "Harness Nexus 3";
  String HARNESS_ARTIFACTORY = "Harness Artifactory";
  String HARNESS_BAMBOO = "Harness Bamboo";
  String HARNESS_DOCKER_REGISTRY = "Harness Docker Registry";
  String HARNESS_GCP_EXPLORATION = "harness-exploration";

  String BUCKET_NAME = "BUCKET_NAME";
  String ARTIFACT_FILE_NAME = "ARTIFACT_FILE_NAME";
  String S3_URL = "S3_URL";
  String DESTINATION_DIR_PATH = "DESTINATION_DIR_PATH";
  Long ARTIFACT_FILE_SIZE = Long.MAX_VALUE;

  String PUBLIC_DNS = "PUBLIC_DNS";
  String ARTIFACTORY_URL = "ARTIFACTORY_URL";
  String ARTIFACT_STREAM_ID_ARTIFACTORY = "ARTIFACT_STREAM_ID_ARTIFACTORY";

  String INTEGRATION_TEST_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

  String CV_CONFIG_ID = "CV_CONFIG_ID";
  String WHITELIST_ID = "WHITELIST_ID";

  String JIRA_CONNECTOR_ID = "JIRA_CONNECTOR_ID";
  String JIRA_ISSUE_ID = "JIRA_ISSUE_ID";
  String APPROVAL_EXECUTION_ID = "APPROVAL_EXECUTION_ID";

  static StaticLimitCheckerWithDecrement mockChecker() {
    return new StaticLimitCheckerWithDecrement() {
      @Override
      public boolean checkAndConsume() {
        return true;
      }

      @Override
      public StaticLimit getLimit() {
        return new io.harness.limits.impl.model.StaticLimit(1000);
      }

      @Override
      public boolean decrement() {
        return true;
      }

      @Override
      public Action getAction() {
        return new Action("invalid-account", ActionType.CREATE_APPLICATION);
      }
    };
  }

  @Value
  @AllArgsConstructor
  class MockChecker implements StaticLimitCheckerWithDecrement {
    private final boolean allowRequest;
    private final ActionType actionType;
    private final StaticLimit limit = new io.harness.limits.impl.model.StaticLimit(1000);

    @Override
    public boolean checkAndConsume() {
      return allowRequest;
    }

    @Override
    public boolean decrement() {
      return true;
    }

    @Override
    public Action getAction() {
      return new Action("invalid-account", actionType);
    }
  }
}
