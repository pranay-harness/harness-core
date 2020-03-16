package io.harness.delegate.task;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// This is temporary solution till all DelegateValidationTasks are moved to
// New Capability Framework. This should go away once that happens.
@UtilityClass
public class CapabilityUtils {
  private static Set<String> taskTypesMigratedToCapabilityFrameworkPhase1 = new HashSet<>(Arrays.asList(
      // Artifact Providers

      "JENKINS", "JENKINS_COLLECTION", "JENKINS_GET_BUILDS", "JENKINS_GET_JOBS", "JENKINS_GET_JOB",
      "JENKINS_GET_ARTIFACT_PATHS", "JENKINS_LAST_SUCCESSFUL_BUILD", "JENKINS_GET_PLANS",
      "JENKINS_VALIDATE_ARTIFACT_SERVER",

      "ECR_GET_BUILDS", "ECR_VALIDATE_ARTIFACT_SERVER", "ECR_GET_PLANS", "ECR_GET_ARTIFACT_PATHS",
      "ECR_VALIDATE_ARTIFACT_STREAM",

      "GCS_GET_ARTIFACT_PATHS", "GCS_GET_BUILDS", "GCS_GET_BUCKETS", "GCS_GET_PLANS",

      "NEXUS_GET_JOBS", "NEXUS_GET_PLANS", "NEXUS_GET_ARTIFACT_PATHS", "NEXUS_GET_GROUP_IDS", "NEXUS_GET_BUILDS",
      "NEXUS_LAST_SUCCESSFUL_BUILD", "NEXUS_COLLECTION", "NEXUS_VALIDATE_ARTIFACT_SERVER",
      "NEXUS_VALIDATE_ARTIFACT_STREAM",

      "AMAZON_S3_COLLECTION", "AMAZON_S3_GET_ARTIFACT_PATHS", "AMAZON_S3_LAST_SUCCESSFUL_BUILD", "AMAZON_S3_GET_BUILDS",
      "AMAZON_S3_GET_PLANS",

      "BAMBOO", "BAMBOO_COLLECTION", "BAMBOO_GET_BUILDS", "BAMBOO_GET_JOBS", "BAMBOO_GET_ARTIFACT_PATHS",
      "BAMBOO_LAST_SUCCESSFUL_BUILD", "BAMBOO_GET_PLANS", "BAMBOO_VALIDATE_ARTIFACT_SERVER",

      "DOCKER_GET_BUILDS", "DOCKER_GET_LABELS", "DOCKER_VALIDATE_ARTIFACT_SERVER", "DOCKER_VALIDATE_ARTIFACT_STREAM",

      "ARTIFACTORY_GET_BUILDS", "ARTIFACTORY_GET_JOBS", "ARTIFACTORY_GET_PLANS", "ARTIFACTORY_GET_ARTIFACTORY_PATHS",
      "ARTIFACTORY_GET_GROUP_IDS", "ARTIFACTORY_LAST_SUCCSSFUL_BUILD", "ARTIFACTORY_COLLECTION",
      "ARTIFACTORY_VALIDATE_ARTIFACT_SERVER", "ARTIFACTORY_VALIDATE_ARTIFACT_STREAM",

      "GCR_GET_BUILDS", "GCR_VALIDATE_ARTIFACT_STREAM", "GCR_GET_PLANS",

      "SFTP_GET_BUILDS", "SFTP_GET_ARTIFACT_PATHS", "SFTP_VALIDATE_ARTIFACT_SERVER",

      "SMB_GET_BUILDS", "SMB_GET_SMB_PATHS", "SMB_VALIDATE_ARTIFACT_SERVER",

      "ACR_GET_BUILDS", "ACR_VALIDATE_ARTIFACT_STREAM", "ACR_GET_PLA  NS", "ACR_GET_ARTIFACT_PATHS",

      // Verification

      "SPLUNK", "SPLUNK_CONFIGURATION_VALIDATE_TASK", "SPLUNK_GET_HOST_RECORDS", "SPLUNK_COLLECT_LOG_DATA",

      "SUMO_COLLECT_LOG_DATA", "SUMO_VALIDATE_CONFIGURATION_TASK", "SUMO_GET_HOST_RECORDS", "SUMO_GET_LOG_DATA_BY_HOST",
      "SUMO_COLLECT_24_7_LOG_DATA",

      "ELK_CONFIGURATION_VALIDATE_TASK", "ELK_COLLECT_LOG_DATA", "ELK_COLLECT_INDICES", "ELK_GET_LOG_SAMPLE",
      "ELK_GET_HOST_RECORDS", "KIBANA_GET_VERSION", "ELK_COLLECT_24_7_LOG_DATA",

      "LOGZ_CONFIGURATION_VALIDATE_TASK", "LOGZ_COLLECT_LOG_DATA", "LOGZ_GET_LOG_SAMPLE", "LOGZ_GET_HOST_RECORDS",

      "APPDYNAMICS_CONFIGURATION_VALIDATE_TASK", "APPDYNAMICS_GET_APP_TASK", "APPDYNAMICS_GET_TIER_TASK",
      "APPDYNAMICS_GET_TIER_MAP", "APPDYNAMICS_COLLECT_METRIC_DATA", "APPDYNAMICS_COLLECT_24_7_METRIC_DATA",
      "APPDYNAMICS_METRIC_DATA_FOR_NODE",

      "NEWRELIC_VALIDATE_CONFIGURATION_TASK", "NEWRELIC_GET_APP_TASK", "NEWRELIC_RESOLVE_APP_TASK",
      "NEWRELIC_RESOLVE_APP_ID_TASK", "NEWRELIC_GET_APP_INSTANCES_TASK", "NEWRELIC_COLLECT_METRIC_DATA",
      "NEWRELIC_COLLECT_24_7_METRIC_DATA", "NEWRELIC_GET_TXNS_WITH_DATA", "NEWRELIC_GET_TXNS_WITH_DATA_FOR_NODE",
      "NEWRELIC_POST_DEPLOYMENT_MARKER",

      "CUSTOM_COLLECT_24_7_LOG_DATA",

      "BUGSNAG_GET_APP_TASK", "BUGSNAG_GET_RECORDS",

      "STACKDRIVER_COLLECT_METRIC_DATA", "STACKDRIVER_METRIC_DATA_FOR_NODE", "STACKDRIVER_LIST_REGIONS",
      "STACKDRIVER_LIST_FORWARDING_RULES", "STACKDRIVER_COLLECT_24_7_METRIC_DATA", "STACKDRIVER_COLLECT_LOG_DATA",
      "STACKDRIVER_COLLECT_24_7_LOG_DATA",

      "APM_VALIDATE_CONNECTOR_TASK", "CUSTOM_LOG_VALIDATE_CONNECTOR_TASK", "APM_GET_TASK",
      "APM_METRIC_DATA_COLLECTION_TASK", "APM_24_7_METRIC_DATA_COLLECTION_TASK", "CUSTOM_LOG_COLLECTION_TASK",

      "DYNA_TRACE_VALIDATE_CONFIGURATION_TASK", "DYNA_TRACE_METRIC_DATA_COLLECTION_TASK",
      "DYNA_TRACE_GET_TXNS_WITH_DATA_FOR_NODE", "DYNATRACE_COLLECT_24_7_METRIC_DATA",

      "PROMETHEUS_VALIDATE_CONFIGURATION_TASK", "PROMETHEUS_METRIC_DATA_COLLECTION_TASK",
      "PROMETHEUS_METRIC_DATA_PER_HOST", "PROMETHEUS_COLLECT_24_7_METRIC_DATA",

      "CLOUD_WATCH_COLLECT_METRIC_DATA", "CLOUD_WATCH_METRIC_DATA_FOR_NODE", "CLOUD_WATCH_COLLECT_24_7_METRIC_DATA",

      // Collaboration Task
      "JIRA", "SERVICENOW_ASYNC", "SERVICENOW_SYNC", "SERVICENOW_VALIDATION",

      "COLLABORATION_PROVIDER_TASK",

      "LDAP_TEST_CONN_SETTINGS", "LDAP_TEST_USER_SETTINGS", "LDAP_TEST_GROUP_SETTINGS", "LDAP_VALIDATE_SETTINGS",
      "LDAP_AUTHENTICATION", "LDAP_SEARCH_GROUPS", "LDAP_FETCH_GROUP",

      // PCF
      "PCF_COMMAND_TASK",

      // Helm
      "HELM_REPO_CONFIG_VALIDATION", "HELM_VALUES_FETCH", "HELM_COMMAND_TASK",

      "ECS_STEADY_STATE_CHECK_TASK", "AWS_ECR_TASK", "AWS_ELB_TASK", "AWS_ECS_TASK", "AWS_IAM_TASK", "AWS_EC2_TASK",
      "AWS_ASG_TASK", "AWS_CODE_DEPLOY_TASK", "AWS_LAMBDA_TASK", "AWS_AMI_ASYNC_TASK", "AWS_CF_TASK",
      "AWS_ROUTE53_TASK", "AMI_GET_BUILDS", "ECS_COMMAND_TASK",

      // Shell Script
      "SCRIPT",

      "COMMAND",

      "HTTP",

      "KUBERNETES_STEADY_STATE_CHECK_TASK", "KUBERNETES_SWAP_SERVICE_SELECTORS_TASK",

      "CLOUD_FORMATION_TASK",

      "CONTAINER_ACTIVE_SERVICE_COUNTS", "CONTROLLER_NAMES_WITH_LABELS", "CONTAINER_CONNECTION_VALIDATION",
      "FETCH_CONTAINER_INFO", "LIST_CLUSTERS",

      "K8S_COMMAND_TASK", "HOST_VALIDATION", "CONNECTIVITY_VALIDATION", "CONTAINER_INFO",

      "TRIGGER_TASK",

      "TERRAFORM_PROVISION_TASK", "TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK", "TERRAFORM_FETCH_TARGETS_TASK",

      "BUILD_SOURCE_TASK"

      // ************* WARNING: do not add tasks to this list ************
      // It is too late for phase 1, add it to phase 2

      ));

  public static boolean isTaskTypeMigratedToCapabilityFrameworkPhase1(String taskType) {
    return taskTypesMigratedToCapabilityFrameworkPhase1.contains(taskType);
  }

  private static Set<String> taskTypesMigratedToCapabilityFrameworkPhase2 = new HashSet<>(Arrays.asList());

  public static boolean isTaskTypeMigratedToCapabilityFrameworkPhase2(String taskType) {
    return taskTypesMigratedToCapabilityFrameworkPhase2.contains(taskType);
  }
}
