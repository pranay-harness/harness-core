package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_LABEL)
public class QLK8sLabelConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLK8sLabel> nodes;
}
