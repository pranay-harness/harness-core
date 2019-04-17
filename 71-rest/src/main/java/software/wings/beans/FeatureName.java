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
  DELEGATE_TASK_VERSIONING,
  LOGML_NEURAL_NET,
  GIT_BATCH_SYNC,
  GLOBAL_CV_DASH,
  LDAP_SSO_PROVIDER,
  CV_SUCCEED_FOR_ANOMALY,
  COPY_ARTIFACT,
  INLINE_SSH_COMMAND,
  LOGIN_PROMPT_WHEN_NO_USER,
  K8S_V2,
  CUSTOM_WORKFLOW,
  ALERT_NOTIFICATIONS,
  ECS_DELEGATE,
  OAUTH_LOGIN,
  USE_QUARTZ_JOBS,
  CV_DATA_COLLECTION_JOB,
  SHELL_SCRIPT_PROVISION,
  THREE_PHASE_SECRET_DECRYPTION,
  DELEGATE_CAPABILITY_FRAMEWORK,
  HARNESS_LITE,
  GRAPHQL,
  SHELL_SCRIPT_ENV,
  REMOVE_STENCILS,
  SERVICENOW,
  DISABLE_METRIC_NAME_CURLY_BRACE_CHECK,
  STACK_DRIVER,
  GLOBAL_DISABLE_HEALTH_CHECK(Scope.GLOBAL),
  AUDIT_TRAIL;

  FeatureName() {
    scope = Scope.PER_ACCOUNT;
  }

  FeatureName(Scope scope) {
    this.scope = scope;
  }

  @Getter private FeatureFlag.Scope scope;
}
