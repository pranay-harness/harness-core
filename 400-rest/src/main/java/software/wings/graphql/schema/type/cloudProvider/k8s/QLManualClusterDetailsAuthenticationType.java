package software.wings.graphql.schema.type.cloudProvider.k8s;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(Module._380_CG_GRAPHQL)
public enum QLManualClusterDetailsAuthenticationType implements QLEnum {
  USERNAME_AND_PASSWORD,
  CLIENT_KEY_AND_CERTIFICATE,
  SERVICE_ACCOUNT_TOKEN,
  OIDC_TOKEN,
  CUSTOM;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
