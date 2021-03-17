package software.wings.graphql.schema.type.usergroup;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Value;

@Value
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUserGroupQueryParameters {
  private String userGroupId;
  private String name;
}
