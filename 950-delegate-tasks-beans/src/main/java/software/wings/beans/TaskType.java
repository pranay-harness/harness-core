package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.TaskGroup;

@TargetModule(Module._955_DELEGATE_BEANS)
public enum TaskType {
  BATCH_CAPABILITY_CHECK(TaskGroup.BATCH_CAPABILITY_CHECK),
  CAPABILITY_VALIDATION(TaskGroup.CAPABILITY_VALIDATION),
  COMMAND(TaskGroup.COMMAND),
  SCRIPT(TaskGroup.SCRIPT),
  HTTP(TaskGroup.HTTP),
  GCB(TaskGroup.GCB),
  JENKINS(TaskGroup.JENKINS),
  JENKINS_COLLECTION(TaskGroup.JENKINS),
  JENKINS_GET_BUILDS(TaskGroup.JENKINS),
  JENKINS_GET_JOBS(TaskGroup.JENKINS),
  JENKINS_GET_JOB(TaskGroup.JENKINS),
  JENKINS_GET_ARTIFACT_PATHS(TaskGroup.JENKINS),
  JENKINS_LAST_SUCCESSFUL_BUILD(TaskGroup.JENKINS),
  JENKINS_GET_PLANS(TaskGroup.JENKINS),
  JENKINS_VALIDATE_ARTIFACT_SERVER(TaskGroup.JENKINS),
  BAMBOO(TaskGroup.BAMBOO),
  BAMBOO_COLLECTION(TaskGroup.BAMBOO),
  BAMBOO_GET_BUILDS(TaskGroup.BAMBOO),
  BAMBOO_GET_JOBS(TaskGroup.BAMBOO),
  BAMBOO_GET_ARTIFACT_PATHS(TaskGroup.BAMBOO),
  BAMBOO_LAST_SUCCESSFUL_BUILD(TaskGroup.BAMBOO),
  BAMBOO_GET_PLANS(TaskGroup.BAMBOO),
  BAMBOO_VALIDATE_ARTIFACT_SERVER(TaskGroup.BAMBOO),
  DOCKER_GET_BUILDS(TaskGroup.DOCKER),
  DOCKER_GET_LABELS(TaskGroup.DOCKER),
  DOCKER_VALIDATE_ARTIFACT_SERVER(TaskGroup.DOCKER),
  DOCKER_VALIDATE_ARTIFACT_STREAM(TaskGroup.DOCKER),
  ECR_GET_BUILDS(TaskGroup.ECR),
  ECR_VALIDATE_ARTIFACT_SERVER(TaskGroup.ECR),
  ECR_GET_PLANS(TaskGroup.ECR),
  ECR_GET_ARTIFACT_PATHS(TaskGroup.ECR),
  ECR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ECR),
  ECR_GET_LABELS(TaskGroup.ECR),
  GCR_GET_BUILDS(TaskGroup.GCR),
  GCR_VALIDATE_ARTIFACT_STREAM(TaskGroup.GCR),
  GCR_GET_PLANS(TaskGroup.GCR),
  ECR_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  ACR_GET_BUILDS(TaskGroup.ACR),
  ACR_VALIDATE_ARTIFACT_STREAM(TaskGroup.ACR),
  ACR_GET_PLANS(TaskGroup.ACR),
  ACR_GET_ARTIFACT_PATHS(TaskGroup.ACR),
  NEXUS_GET_JOBS(TaskGroup.NEXUS),
  NEXUS_GET_PLANS(TaskGroup.NEXUS),
  NEXUS_GET_ARTIFACT_PATHS(TaskGroup.NEXUS),
  NEXUS_GET_GROUP_IDS(TaskGroup.NEXUS),
  NEXUS_GET_BUILDS(TaskGroup.NEXUS),
  NEXUS_LAST_SUCCESSFUL_BUILD(TaskGroup.NEXUS),
  NEXUS_COLLECTION(TaskGroup.NEXUS),
  NEXUS_VALIDATE_ARTIFACT_SERVER(TaskGroup.NEXUS),
  NEXUS_VALIDATE_ARTIFACT_STREAM(TaskGroup.NEXUS),
  GCS_GET_ARTIFACT_PATHS(TaskGroup.GCS),
  GCS_GET_BUILDS(TaskGroup.GCS),
  GCS_GET_BUCKETS(TaskGroup.GCS),
  GCS_GET_PROJECT_ID(TaskGroup.GCS),
  GCS_GET_PLANS(TaskGroup.GCS),
  SFTP_GET_BUILDS(TaskGroup.SFTP),
  SFTP_GET_ARTIFACT_PATHS(TaskGroup.SFTP),
  SFTP_VALIDATE_ARTIFACT_SERVER(TaskGroup.SFTP),
  SMB_GET_BUILDS(TaskGroup.SMB),
  SMB_GET_SMB_PATHS(TaskGroup.SMB),
  SMB_VALIDATE_ARTIFACT_SERVER(TaskGroup.SMB),
  AMAZON_S3_COLLECTION(TaskGroup.S3),
  AMAZON_S3_GET_ARTIFACT_PATHS(TaskGroup.S3),
  AMAZON_S3_LAST_SUCCESSFUL_BUILD(TaskGroup.S3),
  AMAZON_S3_GET_BUILDS(TaskGroup.S3),
  AMAZON_S3_GET_PLANS(TaskGroup.S3),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_SERVER(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_VALIDATE_ARTIFACT_STREAM(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_BUILDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_PROJECTS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_FEEDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_GET_PACKAGES(TaskGroup.AZURE_ARTIFACTS),
  AZURE_ARTIFACTS_COLLECTION(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_VALIDATE_ARTIFACT_SERVER(TaskGroup.AZURE_ARTIFACTS),
  AZURE_MACHINE_IMAGE_GET_BUILDS(TaskGroup.AZURE_ARTIFACTS),
  AZURE_VMSS_COMMAND_TASK(TaskGroup.AZURE_VMSS),
  AZURE_APP_SERVICE_TASK(TaskGroup.AZURE_APP_SERVICE),
  AZURE_ARM_TASK(TaskGroup.AZURE_ARM),
  LDAP_TEST_CONN_SETTINGS(TaskGroup.LDAP),
  LDAP_TEST_USER_SETTINGS(TaskGroup.LDAP),
  LDAP_TEST_GROUP_SETTINGS(TaskGroup.LDAP),
  LDAP_VALIDATE_SETTINGS(TaskGroup.LDAP),
  LDAP_AUTHENTICATION(TaskGroup.LDAP),
  LDAP_SEARCH_GROUPS(TaskGroup.LDAP),
  LDAP_FETCH_GROUP(TaskGroup.LDAP),
  APM_VALIDATE_CONNECTOR_TASK(TaskGroup.APM),
  CUSTOM_LOG_VALIDATE_CONNECTOR_TASK(TaskGroup.LOG),
  APM_GET_TASK(TaskGroup.APM),
  APPDYNAMICS_CONFIGURATION_VALIDATE_TASK(TaskGroup.APPDYNAMICS),
  CVNG_CONNECTOR_VALIDATE_TASK(TaskGroup.CVNG),
  GET_DATA_COLLECTION_RESULT(TaskGroup.CVNG),
  APPDYNAMICS_GET_APP_TASK(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_APP_TASK_NG(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_TASK(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_TASK_NG(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_GET_TIER_MAP(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_METRIC_DATA(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_METRIC_DATA_V2(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  APPDYNAMICS_METRIC_DATA_FOR_NODE(TaskGroup.APPDYNAMICS),
  APPDYNAMICS_METRIC_PACK_DATA(TaskGroup.APPDYNAMICS),
  INSTANA_GET_INFRA_METRICS(TaskGroup.INSTANA),
  INSTANA_GET_TRACE_METRICS(TaskGroup.INSTANA),
  INSTANA_COLLECT_METRIC_DATA(TaskGroup.INSTANA),
  INSTANA_VALIDATE_CONFIGURATION_TASK(TaskGroup.INSTANA),
  NEWRELIC_VALIDATE_CONFIGURATION_TASK(TaskGroup.NEWRELIC),
  BUGSNAG_GET_APP_TASK(TaskGroup.LOG),
  BUGSNAG_GET_RECORDS(TaskGroup.LOG),
  CUSTOM_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  CUSTOM_APM_COLLECT_METRICS_V2(TaskGroup.APM),
  NEWRELIC_GET_APP_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_RESOLVE_APP_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_RESOLVE_APP_ID_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_GET_APP_INSTANCES_TASK(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_METRIC_DATA(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_METRIC_DATAV2(TaskGroup.NEWRELIC),
  NEWRELIC_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  NEWRELIC_GET_TXNS_WITH_DATA(TaskGroup.NEWRELIC),
  NEWRELIC_GET_TXNS_WITH_DATA_FOR_NODE(TaskGroup.NEWRELIC),
  NEWRELIC_POST_DEPLOYMENT_MARKER(TaskGroup.NEWRELIC),
  STACKDRIVER_COLLECT_METRIC_DATA(TaskGroup.STACKDRIVER),
  STACKDRIVER_METRIC_DATA_FOR_NODE(TaskGroup.STACKDRIVER),
  STACKDRIVER_LOG_DATA_FOR_NODE(TaskGroup.STACKDRIVER),
  STACKDRIVER_LIST_REGIONS(TaskGroup.STACKDRIVER),
  STACKDRIVER_LIST_FORWARDING_RULES(TaskGroup.STACKDRIVER),
  STACKDRIVER_GET_LOG_SAMPLE(TaskGroup.STACKDRIVER),
  STACKDRIVER_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  STACKDRIVER_COLLECT_LOG_DATA(TaskGroup.STACKDRIVER),
  STACKDRIVER_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  SPLUNK(TaskGroup.SPLUNK),
  SPLUNK_CONFIGURATION_VALIDATE_TASK(TaskGroup.SPLUNK),
  SPLUNK_GET_HOST_RECORDS(TaskGroup.SPLUNK),
  SPLUNK_NG_GET_SAVED_SEARCHES(TaskGroup.SPLUNK),
  SPLUNK_NG_VALIDATION_RESPONSE_TASK(TaskGroup.SPLUNK),
  SPLUNK_COLLECT_LOG_DATAV2(TaskGroup.SPLUNK),
  ELK_COLLECT_LOG_DATAV2(TaskGroup.ELK),
  DATA_COLLECTION_NEXT_GEN_VALIDATION(TaskGroup.APPDYNAMICS),
  SUMO_COLLECT_LOG_DATA(TaskGroup.SUMO),
  SUMO_VALIDATE_CONFIGURATION_TASK(TaskGroup.SUMO),
  SUMO_GET_HOST_RECORDS(TaskGroup.SUMO),
  SUMO_GET_LOG_DATA_BY_HOST(TaskGroup.SUMO),
  SUMO_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  ELK_CONFIGURATION_VALIDATE_TASK(TaskGroup.ELK),
  ELK_COLLECT_LOG_DATA(TaskGroup.ELK),
  ELK_COLLECT_INDICES(TaskGroup.ELK),
  ELK_GET_LOG_SAMPLE(TaskGroup.ELK),
  ELK_GET_HOST_RECORDS(TaskGroup.ELK),
  KIBANA_GET_VERSION(TaskGroup.ELK),
  ELK_COLLECT_24_7_LOG_DATA(TaskGroup.GUARD_24x7),
  LOGZ_CONFIGURATION_VALIDATE_TASK(TaskGroup.LOGZ),
  LOGZ_COLLECT_LOG_DATA(TaskGroup.LOGZ),
  LOGZ_GET_LOG_SAMPLE(TaskGroup.LOGZ),
  LOGZ_GET_HOST_RECORDS(TaskGroup.ELK),
  ARTIFACTORY_GET_BUILDS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_JOBS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_PLANS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_ARTIFACTORY_PATHS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_GET_GROUP_IDS(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_LAST_SUCCSSFUL_BUILD(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_COLLECTION(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_VALIDATE_ARTIFACT_SERVER(TaskGroup.ARTIFACTORY),
  ARTIFACTORY_VALIDATE_ARTIFACT_STREAM(TaskGroup.ARTIFACTORY),

  // Secret Management (Old Tasks)
  CYBERARK_VALIDATE_CONFIG(TaskGroup.KMS),
  VAULT_GET_CHANGELOG(TaskGroup.KMS),
  VAULT_RENEW_TOKEN(TaskGroup.KMS),
  VAULT_LIST_ENGINES(TaskGroup.KMS),
  VAULT_APPROLE_LOGIN(TaskGroup.KMS),
  SSH_SECRET_ENGINE_AUTH(TaskGroup.KMS),
  VAULT_SIGN_PUBLIC_KEY_SSH(TaskGroup.KMS),
  SECRET_DECRYPT(TaskGroup.KMS),
  BATCH_SECRET_DECRYPT(TaskGroup.KMS),
  SECRET_DECRYPT_REF(TaskGroup.KMS),

  // Secret Management (New Tasks)
  DELETE_SECRET(TaskGroup.KMS),
  VALIDATE_SECRET_REFERENCE(TaskGroup.KMS),
  UPSERT_SECRET(TaskGroup.KMS),
  FETCH_SECRET(TaskGroup.KMS),
  ENCRYPT_SECRET(TaskGroup.KMS),

  HOST_VALIDATION(TaskGroup.HOST_VALIDATION),
  CONTAINER_ACTIVE_SERVICE_COUNTS(TaskGroup.CONTAINER),
  CONTAINER_INFO(TaskGroup.CONTAINER),
  CONTROLLER_NAMES_WITH_LABELS(TaskGroup.CONTAINER),
  AMI_GET_BUILDS(TaskGroup.AMI),
  CONTAINER_CE_VALIDATION(TaskGroup.CONTAINER),
  CE_DELEGATE_VALIDATION(TaskGroup.CONTAINER),
  CONTAINER_CONNECTION_VALIDATION(TaskGroup.CONTAINER),
  LIST_CLUSTERS(TaskGroup.CONTAINER),
  CONTAINER_VALIDATION(TaskGroup.CONTAINER),

  FETCH_MASTER_URL(TaskGroup.CONTAINER),

  DYNA_TRACE_VALIDATE_CONFIGURATION_TASK(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_METRIC_DATA_COLLECTION_TASK(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_GET_TXNS_WITH_DATA_FOR_NODE(TaskGroup.DYNA_TRACE),
  DYNA_TRACE_GET_SERVICES(TaskGroup.DYNA_TRACE),
  DYNATRACE_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  HELM_COMMAND_TASK(TaskGroup.HELM),
  KUBERNETES_STEADY_STATE_CHECK_TASK(TaskGroup.CONTAINER),
  PCF_COMMAND_TASK(TaskGroup.PCF),
  SPOTINST_COMMAND_TASK(TaskGroup.SPOTINST),
  ECS_COMMAND_TASK(TaskGroup.AWS),
  COLLABORATION_PROVIDER_TASK(TaskGroup.COLLABORATION_PROVIDER),
  PROMETHEUS_METRIC_DATA_PER_HOST(TaskGroup.PROMETHEUS),
  CLOUD_WATCH_COLLECT_METRIC_DATA(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_METRIC_DATA_FOR_NODE(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_GENERIC_METRIC_STATISTICS(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_GENERIC_METRIC_DATA(TaskGroup.CLOUD_WATCH),
  CLOUD_WATCH_COLLECT_24_7_METRIC_DATA(TaskGroup.GUARD_24x7),
  APM_METRIC_DATA_COLLECTION_TASK(TaskGroup.APM),

  APM_24_7_METRIC_DATA_COLLECTION_TASK(TaskGroup.GUARD_24x7),

  CUSTOM_LOG_COLLECTION_TASK(TaskGroup.LOG),
  CLOUD_FORMATION_TASK(TaskGroup.CLOUD_FORMATION),
  FETCH_S3_FILE_TASK(TaskGroup.AWS),

  TERRAFORM_PROVISION_TASK(TaskGroup.TERRAFORM),
  TERRAFORM_INPUT_VARIABLES_OBTAIN_TASK(TaskGroup.TERRAFORM),
  TERRAFORM_FETCH_TARGETS_TASK(TaskGroup.TERRAFORM),
  KUBERNETES_SWAP_SERVICE_SELECTORS_TASK(TaskGroup.CONTAINER),
  ECS_STEADY_STATE_CHECK_TASK(TaskGroup.CONTAINER),
  AWS_ECR_TASK(TaskGroup.AWS),
  AWS_ELB_TASK(TaskGroup.AWS),
  AWS_ECS_TASK(TaskGroup.AWS),
  AWS_IAM_TASK(TaskGroup.AWS),
  AWS_EC2_TASK(TaskGroup.AWS),
  AWS_ASG_TASK(TaskGroup.AWS),
  AWS_CODE_DEPLOY_TASK(TaskGroup.AWS),
  AWS_LAMBDA_TASK(TaskGroup.AWS),
  AWS_AMI_ASYNC_TASK(TaskGroup.AWS),
  AWS_CF_TASK(TaskGroup.AWS),
  K8S_COMMAND_TASK(TaskGroup.K8S),
  K8S_COMMAND_TASK_NG(TaskGroup.K8S_NG),
  K8S_WATCH_TASK(TaskGroup.K8S),
  TRIGGER_TASK(TaskGroup.TRIGGER),
  JIRA(TaskGroup.JIRA),
  CONNECTIVITY_VALIDATION(TaskGroup.CONNECTIVITY_VALIDATION),
  GIT_COMMAND(TaskGroup.GIT),
  GIT_FETCH_FILES_TASK(TaskGroup.GIT),
  GIT_FETCH_NEXT_GEN_TASK(TaskGroup.GIT),
  BUILD_SOURCE_TASK(TaskGroup.BUILD_SOURCE),
  DOCKER_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  GCR_ARTIFACT_TASK_NG(TaskGroup.ARTIFACT_COLLECT_NG),
  AWS_ROUTE53_TASK(TaskGroup.AWS),
  SHELL_SCRIPT_APPROVAL(TaskGroup.SCRIPT),
  CUSTOM_GET_BUILDS(TaskGroup.CUSTOM),
  CUSTOM_VALIDATE_ARTIFACT_STREAM(TaskGroup.CUSTOM),
  SHELL_SCRIPT_PROVISION_TASK(TaskGroup.SHELL_SCRIPT_PROVISION),
  SERVICENOW_ASYNC(TaskGroup.SERVICENOW),
  SERVICENOW_SYNC(TaskGroup.SERVICENOW),
  SERVICENOW_VALIDATION(TaskGroup.SERVICENOW),
  HELM_REPO_CONFIG_VALIDATION(TaskGroup.HELM_REPO_CONFIG_VALIDATION),
  HELM_VALUES_FETCH(TaskGroup.HELM_VALUES_FETCH_TASK),
  SLACK(TaskGroup.SLACK),
  CI_BUILD(TaskGroup.CI),
  CI_LE_STATUS(TaskGroup.CI),
  EXECUTE_COMMAND(TaskGroup.CI),
  CI_CLEANUP(TaskGroup.CI),
  AWS_S3_TASK(TaskGroup.AWS),
  CUSTOM_MANIFEST_VALUES_FETCH_TASK(TaskGroup.CUSTOM_MANIFEST_VALUES_FETCH_TASK),

  // Add all NG tasks below this.
  GCP_TASK(TaskGroup.GCP),
  VALIDATE_KUBERNETES_CONFIG(TaskGroup.CONTAINER),
  NG_GIT_COMMAND(TaskGroup.GIT),
  NG_SSH_VALIDATION(TaskGroup.CONNECTIVITY_VALIDATION),
  DOCKER_CONNECTIVITY_TEST_TASK(TaskGroup.DOCKER),
  NG_AWS_TASK(TaskGroup.AWS),
  JIRA_TASK_NG(TaskGroup.JIRA_NG),
  BUILD_STATUS(TaskGroup.CI),
  GIT_API_TASK(TaskGroup.GIT_NG),
  JIRA_CONNECTIVITY_TASK_NG(TaskGroup.JIRA_NG),
  K8_FETCH_NAMESPACES(TaskGroup.CVNG),
  K8_FETCH_WORKLOADS(TaskGroup.CVNG),
  K8_FETCH_EVENTS(TaskGroup.CVNG),
  NOTIFY_SLACK(TaskGroup.NOTIFICATION),
  NOTIFY_PAGERDUTY(TaskGroup.NOTIFICATION),
  NOTIFY_MAIL(TaskGroup.NOTIFICATION),
  NOTIFY_MICROSOFTTEAMS(TaskGroup.NOTIFICATION),
  HTTP_TASK_NG(TaskGroup.HTTP_NG),
  SHELL_SCRIPT_TASK_NG(TaskGroup.SHELL_SCRIPT_NG),
  NG_NEXUS_TASK(TaskGroup.NEXUS),
  NG_ARTIFACTORY_TASK(TaskGroup.ARTIFACTORY),
  CE_VALIDATE_KUBERNETES_CONFIG(TaskGroup.CONTAINER),
  NG_AWS_CODE_COMMIT_TASK(TaskGroup.AWS),
  HTTP_HELM_CONNECTIVITY_TASK(TaskGroup.HELM_REPO_CONFIG_VALIDATION);

  private final TaskGroup taskGroup;

  TaskType(TaskGroup taskGroup) {
    this.taskGroup = taskGroup;
  }

  public TaskGroup getTaskGroup() {
    return taskGroup;
  }
}
