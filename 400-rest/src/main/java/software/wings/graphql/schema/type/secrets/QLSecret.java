package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.SETTING)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface QLSecret extends QLObject {
  String getId();
  QLSecretType getSecretType();
  String getName();
  QLUsageScope getUsageScope();
  default String getSecretReference() {
    return null;
  };
}
