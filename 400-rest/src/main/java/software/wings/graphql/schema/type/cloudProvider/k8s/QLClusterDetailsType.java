package software.wings.graphql.schema.type.cloudProvider.k8s;

import software.wings.graphql.schema.type.QLEnum;

public enum QLClusterDetailsType implements QLEnum {
  INHERIT_CLUSTER_DETAILS,
  MANUAL_CLUSTER_DETAILS;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
