package io.harness.ccm.setup.graphql;

import software.wings.graphql.schema.type.QLObject;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.CE_CLUSTER)
public class QLEKSClusterData implements QLObject {
  private Integer count;
  private List<QLEKSCluster> clusters;
}
