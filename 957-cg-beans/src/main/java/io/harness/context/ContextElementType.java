package io.harness.context;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * Describes what type of element is being repeated on.
 */
@OwnedBy(CDC)
@TargetModule(_957_CG_BEANS)
public enum ContextElementType {
  /**
   * Service context element type.
   */
  SERVICE,

  INFRAMAPPING,
  /**
   * Service context element type.
   */
  SERVICE_TEMPLATE,

  /**
   * Tag context element type.
   */
  TAG,

  /**
   * Shell Script element type
   */
  SHELL,

  /**
   * Host context element type.
   */
  HOST,

  /**
   * Instance context element type.
   */
  INSTANCE,

  /**
   * Standard context element type.
   */
  STANDARD,

  /**
   * Param context element type.
   */
  PARAM,

  /**
   * Partition context element type.
   */
  PARTITION,

  /**
   * Other context element type.
   */
  OTHER,

  /**
   * Fork context element type.
   */
  FORK,

  /**
   * Container cluster - ECS/Kubernetes context element type.
   */
  CONTAINER_SERVICE,

  /**
  /**
   * Cluster context element type.
   */
  CLUSTER,

  /**
   * Aws lambda function context element type.
   */
  AWS_LAMBDA_FUNCTION,

  /**
   * Ami service context element type.
   */
  AMI_SERVICE_SETUP,

  /**
   * Ami service deploy context element type.
   */
  AMI_SERVICE_DEPLOY,

  /**
   * ECS service context element type.
   */
  ECS_SERVICE_SETUP,

  /**
   * Artifact context element type.
   */
  AMI_SWITCH_ROUTES,

  PCF_SERVICE_SETUP,

  PCF_SERVICE_DEPLOY,

  PCF_ROUTE_SWAP_ROLLBACK,

  PCF_INSTANCE,

  SPOTINST_SERVICE_SETUP,

  SPOTINST_SERVICE_DEPLOY,

  ARTIFACT,

  ARTIFACT_VARIABLE,

  /**
   * Helm deploy context element type.
   */
  HELM_DEPLOY,

  CLOUD_FORMATION_PROVISION,

  CLOUD_FORMATION_ROLLBACK,

  CLOUD_FORMATION_DEPROVISION,

  TERRAFORM_PROVISION,

  SHELL_SCRIPT_PROVISION,

  K8S,

  TERRAFORM_INHERIT_PLAN,

  TERRAGRUNT_INHERIT_PLAN,

  AZURE_VMSS_SETUP,

  AZURE_WEBAPP_SETUP,

  HELM_CHART,

  MANIFEST_VARIABLE
}
