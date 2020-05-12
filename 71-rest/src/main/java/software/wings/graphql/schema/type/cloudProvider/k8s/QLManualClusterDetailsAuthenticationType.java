package software.wings.graphql.schema.type.cloudProvider.k8s;

import software.wings.graphql.schema.type.QLEnum;

public enum QLManualClusterDetailsAuthenticationType implements QLEnum {
  USERNAME_AND_PASSWORD,
  CLIENT_KEY_AND_CERTIFICATE,
  SERVICE_ACCOUNT_TOKEN,
  OIDC_TOKEN,
  NONE;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
