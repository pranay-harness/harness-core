package io.harness.configuration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public enum DeployVariant {
  COMMUNITY,
  SAAS;

  public static final String DEPLOY_VERSION = "DEPLOY_VERSION";

  public static boolean isCommunity(String deployVariant) {
    if (isEmpty(deployVariant)) {
      return false;
    }

    return COMMUNITY.name().equals(deployVariant);
  }
}
