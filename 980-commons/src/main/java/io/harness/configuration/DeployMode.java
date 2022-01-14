package io.harness.configuration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

/**
 * Enum to identify the deploymode. Hazelcast , delegate may use this enum for their custom behaviors in respective
 * modes
 */
public enum DeployMode {
  AWS("SAAS"),
  ONPREM("ONPREM"),
  KUBERNETES("SAAS"),
  KUBERNETES_ONPREM("ONPREM");

  private String deployedAs;

  public static final String DEPLOY_MODE = "DEPLOY_MODE";

  DeployMode(String deployedAs) {
    this.deployedAs = deployedAs;
  }

  public String getDeployedAs() {
    return deployedAs;
  }

  public static boolean isOnPrem(String deployMode) {
    if (isEmpty(deployMode)) {
      return false;
    }

    return ONPREM.name().equals(deployMode) || KUBERNETES_ONPREM.name().equals(deployMode);
  }
}
