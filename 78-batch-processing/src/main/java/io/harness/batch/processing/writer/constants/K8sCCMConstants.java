package io.harness.batch.processing.writer.constants;

public class K8sCCMConstants {
  public static final String RELEASE_NAME = "harness.io/release-name";
  public static final String OPERATING_SYSTEM = "beta.kubernetes.io/os";
  public static final String REGION = "failure-domain.beta.kubernetes.io/region";
  public static final String INSTANCE_FAMILY = "beta.kubernetes.io/instance-type";
  public static final String UNALLOCATED = "Unallocated";

  private K8sCCMConstants() {}
}
