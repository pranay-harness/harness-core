package software.wings.beans;

/**
 * Created by rishi on 12/22/16.
 */
public enum PhaseStepType {
  SELECT_NODE,
  PROVISION_NODE,
  DISABLE_SERVICE,
  DEPLOY_SERVICE,
  ENABLE_SERVICE,
  VERIFY_SERVICE,
  WRAP_UP,
  PRE_DEPLOYMENT,
  POST_DEPLOYMENT,
  STOP_SERVICE,
  DE_PROVISION_NODE,
  CLUSTER_SETUP,
  CONTAINER_SETUP,
  CONTAINER_DEPLOY,
  PCF_SETUP,
  PCF_RESIZE,
  PCF_ROUTE_SWAP,
  START_SERVICE,
  DEPLOY_AWSCODEDEPLOY,
  PREPARE_STEPS,
  DEPLOY_AWS_LAMBDA,
  COLLECT_ARTIFACT,
  AMI_AUTOSCALING_GROUP_SETUP,
  AMI_DEPLOY_AUTOSCALING_GROUP,
  HELM_DEPLOY
}
