package software.wings.graphql.schema.type;

import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(ResourceType.APPLICATION)
public class QLArtifactConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLArtifact> nodes;
}
