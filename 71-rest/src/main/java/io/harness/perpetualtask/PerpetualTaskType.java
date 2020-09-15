package io.harness.perpetualtask;

public final class PerpetualTaskType {
  public static final String K8S_WATCH = "K8S_WATCH";
  public static final String ECS_CLUSTER = "ECS_CLUSTER";
  public static final String SAMPLE = "SAMPLE";
  public static final String ARTIFACT_COLLECTION = "ARTIFACT_COLLECTION";
  public static final String PCF_INSTANCE_SYNC = "PCF_INSTANCE_SYNC";
  public static final String AWS_SSH_INSTANCE_SYNC = "AWS_SSH_INSTANCE_SYNC";
  public static final String AWS_AMI_INSTANCE_SYNC = "AWS_AMI_INSTANCE_SYNC";
  public static final String AWS_CODE_DEPLOY_INSTANCE_SYNC = "AWS_CODE_DEPLOY_INSTANCE_SYNC";
  public static final String SPOT_INST_AMI_INSTANCE_SYNC = "SPOT_INST_AMI_INSTANCE_SYNC";
  public static final String CONTAINER_INSTANCE_SYNC = "CONTAINER_INSTANCE_SYNC";
  public static final String AWS_LAMBDA_INSTANCE_SYNC = "AWS_LAMBDA_INSTANCE_SYNC";
  public static final String CUSTOM_DEPLOYMENT_INSTANCE_SYNC = "CUSTOM_DEPLOYMENT_INSTANCE_SYNC";
  public static final String DATA_COLLECTION_TASK = "DATA_COLLECTION_TASK";
  public static final String AZURE_VMSS_INSTANCE_SYNC = "AZURE_VMSS_INSTANCE_SYNC";
  public static final String MANIFEST_COLLECTION = "MANIFEST_COLLECTION";

  private PerpetualTaskType() {}
}
