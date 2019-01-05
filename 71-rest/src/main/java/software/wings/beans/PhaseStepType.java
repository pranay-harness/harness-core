package software.wings.beans;

/**
 * Created by rishi on 12/22/16.
 */
public enum PhaseStepType {
  SELECT_NODE,
  INFRASTRUCTURE_NODE,
  @Deprecated PROVISION_NODE,
  DISABLE_SERVICE,
  DEPLOY_SERVICE,
  ENABLE_SERVICE,
  VERIFY_SERVICE,
  WRAP_UP,
  PRE_DEPLOYMENT,
  ROLLBACK_PROVISIONERS,
  POST_DEPLOYMENT,
  STOP_SERVICE,
  @Deprecated DE_PROVISION_NODE,
  CLUSTER_SETUP,
  CONTAINER_SETUP,
  CONTAINER_DEPLOY,
  PCF_SETUP,
  PCF_RESIZE,
  PCF_ROUTE_UPDATE,
  PCF_SWICH_ROUTES,
  START_SERVICE,
  DEPLOY_AWSCODEDEPLOY,
  PREPARE_STEPS,
  DEPLOY_AWS_LAMBDA,
  COLLECT_ARTIFACT,
  AMI_AUTOSCALING_GROUP_SETUP,
  AMI_DEPLOY_AUTOSCALING_GROUP,
  AMI_SWITCH_AUTOSCALING_GROUP_ROUTES,
  ECS_UPDATE_LISTENER_BG,
  HELM_DEPLOY,
  ROUTE_UPDATE,
  K8S_PHASE_STEP
}
