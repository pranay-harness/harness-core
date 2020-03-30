package software.wings.beans;

import lombok.Getter;
import software.wings.beans.FeatureFlag.Scope;

/**
 * Add your feature name here. When the feature is fully launched and no longer needs to be flagged,
 * delete the feature name.
 */
public enum FeatureName {
  CV_DEMO,
  DISABLE_LOGML_NEURAL_NET,
  GIT_BATCH_SYNC,
  GLOBAL_CV_DASH,
  CV_SUCCEED_FOR_ANOMALY,
  COPY_ARTIFACT,
  INLINE_SSH_COMMAND,
  CV_DATA_COLLECTION_JOB,
  DELEGATE_CAPABILITY_FRAMEWORK,
  DELEGATE_CAPABILITY_FRAMEWORK_PHASE1_ENABLE,
  DELEGATE_CAPABILITY_FRAMEWORK_PHASE2_ENABLE,
  GRAPHQL,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  GIT_HTTPS_KERBEROS,
  TRIGGER_FOR_ALL_ARTIFACTS,
  USE_PCF_CLI,
  ARTIFACT_STREAM_REFACTOR,
  TRIGGER_REFACTOR,
  TRIGGER_YAML,
  CV_FEEDBACKS,
  CV_HOST_SAMPLING,
  CUSTOM_DASHBOARD,
  SEND_LOG_ANALYSIS_COMPRESSED,
  INFRA_MAPPING_REFACTOR,
  GRAPHQL_DEV,
  SUPERVISED_TS_THRESHOLD,
  REJECT_TRIGGER_IF_ARTIFACTS_NOT_MATCH,
  NEW_INSTANCE_TIMESERIES,
  ENTITY_AUDIT_RECORD,
  TIME_RANGE_FREEZE_GOVERNANCE,
  SCIM_INTEGRATION,
  NEW_RELIC_CV_TASK,
  SLACK_APPROVALS,
  NEWRELIC_24_7_CV_TASK,
  SEARCH(Scope.GLOBAL),
  PIPELINE_GOVERNANCE,
  TEMPLATED_PIPELINES,
  SEARCH_REQUEST,
  ON_DEMAND_ROLLBACK,
  NODE_AGGREGATION,
  BIND_FETCH_FILES_TASK_TO_DELEGATE,
  DEFAULT_ARTIFACT,
  DEPLOY_TO_SPECIFIC_HOSTS,
  UI_ALLOW_K8S_V1,
  SEND_SLACK_NOTIFICATION_FROM_DELEGATE,
  LOGS_V2_247,
  WEEKLY_WINDOW,
  SWITCH_GLOBAL_TO_GCP_KMS,
  SIDE_NAVIGATION,
  WORKFLOW_TS_RECORDS_NEW,
  /* This is marked deprecated as this feature is not used anywhere except a migration. Waiting for all the customers
   * for migration to run before removing the feature flag.*/
  @Deprecated TEMPLATE_YAML_SUPPORT,
  CUSTOM_APM_CV_TASK,
  CUSTOM_APM_24_X_7_CV_TASK,
  VANITY_URL,
  GIT_SYNC_REFACTOR,
  DISABLE_SERVICEGUARD_LOG_ALERTS,
  ARTIFACT_PERPETUAL_TASK,
  DEPLOYMENT_TAGS,
  REMOVE_FEEDBACK_CRON,
  USE_CDN_FOR_STORAGE_FILES,
  LOCAL_DELEGATE_CONFIG_OVERRIDE,
  REVALIDATE_WHITELISTED_DELEGATE,
  UPGRADE_JRE,
  APPD_CV_TASK,

  /*
  We have 3 phases for expression decryption. We using the two feature flags on controlling the behavior of enabling
  decryption.
  Phase 1 is when the task is about to be queued:
      no feature - "${myvariable.that.is.secret"}                   -> "my secret"
      two || three                                                  -> "${secretManager.obtain("secret key", token)}"

  2. pre execution of task
          no feature - "my secret"                                  -> "my secret"
          two - "${secretManager.obtain("secret key", token)}"      -> "my secret"
          three - "${secretManager.obtain("secret key", token)}"    -> "${secretDelegate.obtain("secret key", token)}"

  3. Run on delegate
          no feature & two - "my secret"                            -> "my secret"
          three - "${secretDelegate.obtain("secret key", token)}"   -> "my secret"
  */
  TWO_PHASE_SECRET_DECRYPTION,
  THREE_PHASE_SECRET_DECRYPTION,
  GLOBAL_COMMAND_LIBRARY,
  DELEGATE_TAGS_EXTENDED,
  CONNECTORS_REF_SECRETS;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
