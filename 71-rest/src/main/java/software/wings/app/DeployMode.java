package software.wings.app;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

/**
 * Enum to identify the deploymode. Hazelcast , delegate may use this enum for their custom behaviors in respective
 * modes
 */
public enum DeployMode {
  AWS,
  ONPREM,
  KUBERNETES,
  KUBERNETES_ONPREM;

  public static boolean isOnPrem(String deployMode) {
    if (isEmpty(deployMode)) {
      return false;
    }

    return ONPREM.name().equals(deployMode) || KUBERNETES_ONPREM.name().equals(deployMode);
  }
}
