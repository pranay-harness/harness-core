package software.wings.graphql.schema.type.usergroup;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(Module._380_CG_GRAPHQL)
public interface QLLinkedSSOSetting extends QLObject {
  String getSsoProviderId();
}
