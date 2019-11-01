package software.wings.beans;

import lombok.Getter;
import software.wings.beans.FeatureFlag.Scope;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
public enum FeatureName {
  AWS_CLOUD_FORMATION_TEMPLATE,
  CV_DEMO,
  LOGML_NEURAL_NET,
  GIT_BATCH_SYNC,
  GLOBAL_CV_DASH,
  LDAP_SSO_PROVIDER,
  CV_SUCCEED_FOR_ANOMALY,
  COPY_ARTIFACT,
  INLINE_SSH_COMMAND,
  LOGIN_PROMPT_WHEN_NO_USER,
  CUSTOM_WORKFLOW,
  ALERT_NOTIFICATIONS,
  ECS_DELEGATE,
  OAUTH_LOGIN,
  USE_QUARTZ_JOBS,
  CV_DATA_COLLECTION_JOB,
  THREE_PHASE_SECRET_DECRYPTION,
  DELEGATE_CAPABILITY_FRAMEWORK,
  HARNESS_LITE,
  GRAPHQL,
  SHELL_SCRIPT_ENV,
  REMOVE_STENCILS,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GIT_HTTPS_KERBEROS,
  TRIGGER_FOR_ALL_ARTIFACTS,
  USE_PCF_CLI,
  AUDIT_TRAIL_UI,
  ARTIFACT_STREAM_REFACTOR,
  TRIGGER_REFACTOR,
  TRIGGER_YAML,
  CV_FEEDBACKS,
  CV_TASKS,
  CV_ACTIVITY_LOG(Scope.GLOBAL),
  CUSTOM_DASHBOARD,
  SEND_LOG_ANALYSIS_COMPRESSED,
  SSH_SHORT_VALIDATION_TIMEOUT,
  PERPETUAL_TASK_SERVICE(Scope.GLOBAL),
  CCM_EVENT_COLLECTION,
  INFRA_MAPPING_REFACTOR,
  GLOBAL_HARNESS_USER_GROUP(Scope.GLOBAL),
  GCP_MARKETPLACE_INTEGRATION,
  GRAPHQL_DEV,
  SUPERVISED_TS_THRESHOLD,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  TAGS_YAML,
  NEW_INSTANCE_TIMESERIES,
  SPOTINST,
  ENTITY_AUDIT_RECORD,
  TIME_RANGE_FREEZE_GOVERNANCE(Scope.PER_ACCOUNT),
  SCIM_INTEGRATION,
  SPLUNK_CV_TASK,
  NEW_RELIC_CV_TASK,
  SLACK_APPROVALS,
  SPLUNK_24_7_CV_TASK,
  NEWRELIC_24_7_CV_TASK,
  SEARCH(Scope.GLOBAL),
  GRAPHQL_STRESS_TESTING,
  PCF_MANIFEST_REDESIGN,
  SERVERLESS_DASHBOARD_AWS_LAMBDA,
  GLOBAL_KMS_PRE_PROCESSING,
  DEPLOYMENT_MODAL_REFACTOR,
  BATCH_SECRET_DECRYPTION,
  PIPELINE_GOVERNANCE,
  ADD_COMMAND,
  STACKDRIVER_SERVICEGUARD,
  SEARCH_REQUEST,
  ON_DEMAND_ROLLBACK;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
