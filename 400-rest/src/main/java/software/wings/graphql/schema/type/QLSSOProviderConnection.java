package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SSO)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLSSOProviderConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLSSOProvider> nodes;
}
