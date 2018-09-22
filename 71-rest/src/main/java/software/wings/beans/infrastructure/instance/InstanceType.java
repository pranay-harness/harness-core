package software.wings.beans.infrastructure.instance;

/**
 * @author rktummala on 09/07/17
 */
public enum InstanceType {
  PHYSICAL_HOST_INSTANCE,
  EC2_CLOUD_INSTANCE,
  GCP_CLOUD_INSTANCE,
  ECS_CONTAINER_INSTANCE,
  KUBERNETES_CONTAINER_INSTANCE,
  PCF_INSTANCE
}
