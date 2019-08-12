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
  TRIGGER_YAML,
  CV_FEEDBACKS,
  CV_TASKS,
  CV_ACTIVITY_LOG(Scope.GLOBAL),
  CUSTOM_DASHBOARD,
  SEND_LOG_ANALYSIS_COMPRESSED,
  SSH_SHORT_VALIDATION_TIMEOUT,
  PERPETUAL_TASK_SERVICE(Scope.GLOBAL),
  INFRA_MAPPING_REFACTOR,
  GLOBAL_HARNESS_USER_GROUP(Scope.GLOBAL),
  GCP_MARKETPLACE_INTEGRATION,
  GRAPHQL_DEV,
  SUPERVISED_TS_THRESHOLD,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  TAGS_YAML,
  TF_ALL,
  NEW_INSTANCE_TIMESERIES,
  SPOTINST;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
