package software.wings.graphql.schema.type;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTagKeys")
@Scope(ResourceType.SETTING)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTag implements QLObject {
  String name;
  String value;
}
