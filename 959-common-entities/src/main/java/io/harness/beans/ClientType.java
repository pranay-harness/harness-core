package io.harness.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

public enum ClientType {
  SALESFORCE(AuthType.API_KEY_HEADER),
  PROMETHEUS(AuthType.AUTH_HEADER),
  IDENTITY_SERVICE(AuthType.AUTH_TOKEN_HEADER),
  INTERNAL(AuthType.API_KEY_HEADER);

  private final AuthType authType;

  ClientType(AuthType authType) {
    this.authType = authType;
  }

  public AuthType getAuthType() {
    return authType;
  }

  public static boolean isValid(String clientType) {
    if (isEmpty(clientType)) {
      return false;
    }
    if (SALESFORCE.name().equalsIgnoreCase(clientType) || PROMETHEUS.name().equalsIgnoreCase(clientType)
        || INTERNAL.name().equalsIgnoreCase(clientType) || IDENTITY_SERVICE.name().equalsIgnoreCase(clientType)) {
      return true;
    }
    return false;
  }
}
