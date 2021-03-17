package software.wings.graphql.schema.type.cloudProvider.k8s;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(Module._380_CG_GRAPHQL)
public enum QLClusterDetailsType implements QLEnum {
  INHERIT_CLUSTER_DETAILS,
  MANUAL_CLUSTER_DETAILS;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
