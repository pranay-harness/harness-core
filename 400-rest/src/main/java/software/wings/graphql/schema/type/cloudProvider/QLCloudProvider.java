package software.wings.graphql.schema.type.cloudProvider;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Scope(ResourceType.SETTING)
@TargetModule(Module._380_CG_GRAPHQL)
public interface QLCloudProvider extends QLObject {
  String getId();
}
