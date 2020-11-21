package io.harness.encryption;

import io.harness.utils.FullyQualifiedIdentifierHelper;

import lombok.experimental.UtilityClass;

@UtilityClass
public class SecretRefDataHelper {
  public String getFullyQualifiedSecretRefString(
      SecretRefData secretRefData, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    if (secretRefData == null) {
      return null;
    }

    return FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, secretRefData.getIdentifier());
  }
}
