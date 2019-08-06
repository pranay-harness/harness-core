package software.wings.beans;

import lombok.Getter;

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
  PRE_DEPLOYMENT("Pre-Deployment"),
  ROLLBACK_PROVISIONERS,
  POST_DEPLOYMENT("Post-Deployment"),
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
  ECS_UPDATE_ROUTE_53_DNS_WEIGHT,
  HELM_DEPLOY,
  ROUTE_UPDATE,
  K8S_PHASE_STEP,
  PROVISION_INFRASTRUCTURE,
  ROLLBACK_PROVISION_INFRASTRUCTURE,
  SPOTINST_SETUP,
  SPOTINST_DEPLOY,
  SPOTINST_LISTENER_UPDATE;

  PhaseStepType() {}

  PhaseStepType(String defaultName) {
    this.defaultName = defaultName;
  }

  @Getter private String defaultName;
}
