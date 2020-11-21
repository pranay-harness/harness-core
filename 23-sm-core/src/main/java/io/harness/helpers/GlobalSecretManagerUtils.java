package io.harness.helpers;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.NGSecretManagerMetadata;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class GlobalSecretManagerUtils {
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  public static boolean isNgHarnessSecretManager(NGSecretManagerMetadata ngSecretManagerMetadata) {
    return ngSecretManagerMetadata != null && ngSecretManagerMetadata.getIdentifier() != null
        && HARNESS_SECRET_MANAGER_IDENTIFIER.equals(ngSecretManagerMetadata.getIdentifier());
  }
}
