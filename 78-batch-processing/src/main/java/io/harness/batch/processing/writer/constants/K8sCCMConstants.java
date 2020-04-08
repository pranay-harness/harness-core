package io.harness.batch.processing.writer.constants;

public class K8sCCMConstants {
  public static final String AWS_LIFECYCLE_KEY = "lifecycle";
  public static final String RELEASE_NAME = "harness.io/release-name";
  public static final String HELM_RELEASE_NAME = "release";
  public static final String K8SV1_RELEASE_NAME = "harness.io/service-infra-id";
  public static final String OPERATING_SYSTEM = "beta.kubernetes.io/os";
  public static final String PREEMPTIBLE_KEY = "preemptible";
  public static final String REGION = "failure-domain.beta.kubernetes.io/region";
  public static final String ZONE = "failure-domain.beta.kubernetes.io/zone";
  public static final String INSTANCE_FAMILY = "beta.kubernetes.io/instance-type";
  public static final String UNALLOCATED = "Unallocated";
  public static final String DEFAULT_DEPLOYMENT_TYPE = "Pod";

  private K8sCCMConstants() {}
}
