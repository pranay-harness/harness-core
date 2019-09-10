package software.wings.common;

public interface WorkflowConstants {
  String K8S_DEPLOYMENT_ROLLING = "Rollout Deployment";
  String K8S_DEPLOYMENT_ROLLING_ROLLBACK = "Rollback Deployment";
  String K8S_BLUE_GREEN_DEPLOY = "Blue/Green Deployment";
  String K8S_SCALE = "Scale";
  String K8S_DELETE = "Delete";
  String K8S_CANARY_DEPLOY = "Canary Deployment";
  String K8S_STAGE_DEPLOY = "Stage Deployment";

  String K8S_PRIMARY_PHASE_NAME = "Primary";
  String K8S_CANARY_PHASE_NAME = "Canary";
}
