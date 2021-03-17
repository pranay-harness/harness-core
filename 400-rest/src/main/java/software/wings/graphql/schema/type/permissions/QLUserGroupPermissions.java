package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUserGroupPermissions {
  QLAccountPermissions accountPermissions;
  List<QLAppPermission> appPermissions;
}
