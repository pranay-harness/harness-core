package software.wings.graphql.schema.type.cloudProvider;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCloudProviderConnectionKeys")
@Scope(ResourceType.SETTING)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLCloudProviderConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLCloudProvider> nodes;
}
