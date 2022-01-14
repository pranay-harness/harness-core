package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.ssoProvider.QLSSOType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSSOProviderKeys")
@Scope(PermissionAttribute.ResourceType.SSO)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSSOProvider implements QLObject {
  String id;
  String name;
  QLSSOType ssoType;
}
