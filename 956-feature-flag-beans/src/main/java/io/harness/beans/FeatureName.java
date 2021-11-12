package io.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag.Scope;

import lombok.Getter;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
@OwnedBy(HarnessTeam.PL)
public enum FeatureName {
  APP_TELEMETRY,
  APPD_CV_TASK,
  ARGO_PHASE1,
  ARGO_PHASE2_MANAGED,
  ARTIFACT_PERPETUAL_TASK,
  ARTIFACT_PERPETUAL_TASK_MIGRATION,
  ARTIFACT_STREAM_REFACTOR,
  ARTIFACT_STREAM_DELEGATE_SCOPING,
  ARTIFACT_STREAM_DELEGATE_TIMEOUT,
  AUTO_ACCEPT_SAML_ACCOUNT_INVITES,
  AZURE_US_GOV_CLOUD,
  AZURE_VMSS,
  AZURE_WEBAPP,
  AZURE_ARM,
  AUDIT_TRAIL_ENHANCEMENT,
  BIND_FETCH_FILES_TASK_TO_DELEGATE,
  BUSINESS_MAPPING,
  CDNG_ENABLED,
  CENG_ENABLED,
  CE_AS_KUBERNETES_ENABLED,
  CE_ANOMALY_DETECTION,
  CE_INVENTORY_DASHBOARD,
  CE_BILLING_DATA_PRE_AGGREGATION,
  CE_BILLING_DATA_HOURLY_PRE_AGGREGATION,
  CE_SAMPLE_DATA_GENERATION,
  CE_AZURE_SUPPORT,
  CFNG_ENABLED,
  CF_CUSTOM_EXTRACTION,
  CING_ENABLED,
  CI_INDIRECT_LOG_UPLOAD,
  CLOUD_FORMATION_CREATE_REFACTOR,
  CUSTOM_APM_24_X_7_CV_TASK,
  CUSTOM_APM_CV_TASK,
  CUSTOM_DASHBOARD,
  CUSTOM_DEPLOYMENT,
  CUSTOM_MAX_PAGE_SIZE,
  CUSTOM_SECRETS_MANAGER,
  CVNG_ENABLED,
  CV_DEMO,
  CV_FEEDBACKS,
  CV_HOST_SAMPLING,
  CV_SUCCEED_FOR_ANOMALY,
  DEFAULT_ARTIFACT,
  DEPLOY_TO_SPECIFIC_HOSTS,
  ENABLE_ADDING_SERVICE_VARS_TO_ECS_SPEC,
  DISABLE_LOGML_NEURAL_NET,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  DISABLE_SERVICEGUARD_LOG_ALERTS,
  DISABLE_WINRM_COMMAND_ENCODING,
  ENABLE_WINRM_ENV_VARIABLES,
  FF_PIPELINE,
  FF_GITSYNC,
  WINRM_COPY_CONFIG_OPTIMIZE,
  ECS_MULTI_LBS,
  ENTITY_AUDIT_RECORD,
  EXPORT_TF_PLAN,
  GCB_CI_SYSTEM,
  GCP_WORKLOAD_IDENTITY,
  GIT_ACCOUNT_SUPPORT,
  GIT_HTTPS_KERBEROS,
  GIT_HOST_CONNECTIVITY,
  GLOBAL_COMMAND_LIBRARY,
  GLOBAL_CV_DASH,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GRAPHQL_DEV,
  HARNESS_TAGS,
  HELM_CHART_AS_ARTIFACT,
  HELM_STEADY_STATE_CHECK_1_16,
  HELM_CHART_NAME_SPLIT,
  HELM_MERGE_CAPABILITIES,
  INLINE_SSH_COMMAND,
  IGNORE_PCF_CONNECTION_CONTEXT_CACHE,
  LIMIT_PCF_THREADS,
  OPA_PIPELINE_GOVERNANCE,
  PCF_OLD_APP_RESIZE,
  LOCAL_DELEGATE_CONFIG_OVERRIDE,
  LOGS_V2_247,
  MOVE_AWS_AMI_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_AMI_SPOT_INST_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_CODE_DEPLOY_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_LAMBDA_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_AWS_SSH_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_CONTAINER_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  MOVE_PCF_INSTANCE_SYNC_TO_PERPETUAL_TASK,
  NEW_INSTANCE_TIMESERIES,
  NEW_RELIC_CV_TASK,
  NEWRELIC_24_7_CV_TASK,
  NG_DASHBOARDS,
  NG_CG_TASK_ASSIGNMENT_ISOLATION,
  NODE_RECOMMENDATION_1,
  NODE_RECOMMENDATION_AGGREGATE,
  ON_NEW_ARTIFACT_TRIGGER_WITH_LAST_COLLECTED_FILTER,
  OUTAGE_CV_DISABLE,
  OVERRIDE_VALUES_YAML_FROM_HELM_CHART,
  PIPELINE_GOVERNANCE,
  PRUNE_KUBERNETES_RESOURCES,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  ROLLBACK_NONE_ARTIFACT,
  SCIM_INTEGRATION,
  SEARCH(Scope.GLOBAL),
  SEARCH_REQUEST,
  SEND_LOG_ANALYSIS_COMPRESSED,
  SEND_SLACK_NOTIFICATION_FROM_DELEGATE,
  SIDE_NAVIGATION,
  SKIP_SWITCH_ACCOUNT_REAUTHENTICATION,
  SLACK_APPROVALS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_AMI_SPOT_INST_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_CODE_DEPLOY_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_LAMBDA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AWS_SSH_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_AZURE_INFRA_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_CONTAINER_DEPLOYMENTS,
  STOP_INSTANCE_SYNC_VIA_ITERATOR_FOR_PCF_DEPLOYMENTS,
  SUPERVISED_TS_THRESHOLD,
  TEMPLATIZED_SECRET_MANAGER,
  TERRAGRUNT,
  THREE_PHASE_SECRET_DECRYPTION,
  TIME_RANGE_FREEZE_GOVERNANCE,
  TRIGGER_FOR_ALL_ARTIFACTS,
  TRIGGER_YAML,
  UI_ALLOW_K8S_V1,
  USE_NEXUS3_PRIVATE_APIS,
  WEEKLY_WINDOW,
  ENABLE_CVNG_INTEGRATION,
  DYNATRACE_MULTI_SERVICE,
  REFACTOR_STATEMACHINEXECUTOR,
  WORKFLOW_DATA_COLLECTION_ITERATOR,
  HELM_REMOTE_MANIFEST_COMMAND_FLAG,
  ENABLE_CERT_VALIDATION,
  RESOURCE_CONSTRAINT_MAX_QUEUE,
  HIDE_SCOPE_COMMAND_OPTION,
  AWS_OVERRIDE_REGION,
  SHOW_TASK_SETUP_ABSTRACTIONS,
  CLEAN_UP_OLD_MANAGER_VERSIONS(Scope.PER_ACCOUNT),
  ECS_AUTOSCALAR_REDESIGN,
  SAVE_SHELL_SCRIPT_PROVISION_OUTPUTS_TO_SWEEPING_OUTPUT,
  SAVE_TERRAFORM_OUTPUTS_TO_SWEEPING_OUTPUT,
  TRIGGER_PROFILE_SCRIPT_EXECUTION_WF,
  NEW_DEPLOYMENT_FREEZE,
  PER_AGENT_CAPABILITIES,
  ECS_REGISTER_TASK_DEFINITION_TAGS,
  CUSTOM_DASHBOARD_INSTANCE_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_DEPLOYMENT_FETCH_LONGER_RETENTION_DATA,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_INSTANCE_AGGREGATION,
  CUSTOM_DASHBOARD_ENABLE_REALTIME_DEPLOYMENT_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_INSTANCE_DATA_MIGRATION,
  CUSTOM_DASHBOARD_ENABLE_CRON_DEPLOYMENT_DATA_MIGRATION,
  SSH_SECRET_ENGINE,
  WHITELIST_PUBLIC_API,
  WHITELIST_GRAPHQL,
  TIMEOUT_FAILURE_SUPPORT,
  LOG_APP_DEFAULTS,
  ENABLE_LOGIN_AUDITS,
  CUSTOM_MANIFEST,
  WEBHOOK_TRIGGER_AUTHORIZATION,
  NG_HELM_SOURCE_REPO,
  ENHANCED_GCR_CONNECTIVITY_CHECK,
  USE_TF_CLIENT,
  SERVICE_DASHBOARD_NG,
  GITHUB_WEBHOOK_AUTHENTICATION,
  NG_SIGNUP(Scope.GLOBAL),
  NG_LICENSES_ENABLED(Scope.GLOBAL),
  ECS_BG_DOWNSIZE,
  LIMITED_ACCESS_FOR_HARNESS_USER_GROUP,
  REMOVE_STENCIL_MANUAL_INTERVENTION,
  CI_OVERVIEW_PAGE,
  CD_OVERVIEW_PAGE,
  USE_CUSTOM_DELEGATE_TOKENS,
  SKIP_BASED_ON_STACK_STATUSES,
  WF_VAR_MULTI_SELECT_ALLOWED_VALUES,
  LDAP_GROUP_SYNC_JOB_ITERATOR,
  PIPELINE_MONITORING,
  CF_CLI7,
  CF_APP_NON_VERSIONING_INACTIVE_ROLLBACK,
  CF_ALLOW_SPECIAL_CHARACTERS,
  HTTP_HEADERS_CAPABILITY_CHECK,
  AMI_IN_SERVICE_HEALTHY_WAIT,
  SETTINGS_OPTIMIZATION,
  CG_SECRET_MANAGER_DELEGATE_SELECTORS,
  ARTIFACT_COLLECTION_CONFIGURABLE,
  ROLLBACK_PROVISIONER_AFTER_PHASES,
  PLANS_ENABLED,
  FEATURE_ENFORCEMENT_ENABLED,
  FREE_PLAN_ENABLED,
  NG_GIT_FULL_SYNC,
  NG_DASHBOARD_LANDING_PAGE,
  SOCKET_HTTP_STATE_TIMEOUT,
  TERRAFORM_CONFIG_INSPECT_VERSION_SELECTOR,
  WORKFLOW_PIPELINE_PERMISSION_BY_ENTITY,
  AMAZON_ECR_AUTH_REFACTOR,
  AMI_ASG_CONFIG_COPY,
  RETRY_FAILED_PIPELINE,
  OPTIMIZED_GIT_FETCH_FILES,
  CVNG_VERIFY_STEP_DEMO,
  CVNG_MONITORED_SERVICE_DEMO,
  MANIFEST_INHERIT_FROM_CANARY_TO_PRIMARY_PHASE,
  USE_LATEST_CHARTMUSEUM_VERSION,
  KUBERNETES_EXPORT_MANIFESTS,
  NG_TEMPLATES,
  RUN_INDIVIDUAL_STAGE,
  VARIABLE_SUPPORT_FOR_KUSTOMIZE,
  CVNG_VERIFY_STEP_TO_SINGLE_ACTIVITY,
  FAIL_TASKS_IF_DELEGATE_DIES,
  SSH_JSCH_LOGS,
  NG_GIT_ERROR_EXPERIENCE,
  RESOLVE_DEPLOYMENT_TAGS_BEFORE_EXECUTION,
  LDAP_USER_ID_SYNC,
  NEW_KUBECTL_VERSION,
  CUSTOM_DASHBOARD_V2, // To be used only by ui to control flow from cg dashbaords to ng
  TIME_SCALE_CG_SYNC,
  DELEGATE_SELECTION_LOGS_DISABLED,
  DISABLE_DEPLOYMENTS_SEARCH_AND_LIMIT_DEPLOYMENT_STATS;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
