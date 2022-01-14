package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApiKeyKeys")
@Scope(PermissionAttribute.ResourceType.API_KEY)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLApiKey implements QLObject {
  private String id;
  private String name;
}
