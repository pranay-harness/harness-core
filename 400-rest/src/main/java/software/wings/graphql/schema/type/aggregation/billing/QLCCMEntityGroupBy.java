package software.wings.graphql.schema.type.aggregation.billing;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLAggregationKind;

@TargetModule(Module._380_CG_GRAPHQL)
public enum QLCCMEntityGroupBy {
  Application(QLAggregationKind.SIMPLE),
  Service(QLAggregationKind.SIMPLE),
  Cluster(QLAggregationKind.SIMPLE), // for cluster id
  StartTime(QLAggregationKind.SIMPLE),
  Region(QLAggregationKind.SIMPLE),
  Environment(QLAggregationKind.SIMPLE),
  CloudServiceName(QLAggregationKind.SIMPLE),
  TaskId(QLAggregationKind.SIMPLE),
  LaunchType(QLAggregationKind.SIMPLE),
  WorkloadName(QLAggregationKind.SIMPLE),
  WorkloadType(QLAggregationKind.SIMPLE),
  Namespace(QLAggregationKind.SIMPLE),
  ClusterType(QLAggregationKind.SIMPLE),
  ClusterName(QLAggregationKind.SIMPLE),
  InstanceType(QLAggregationKind.SIMPLE),
  InstanceName(QLAggregationKind.SIMPLE),
  CloudProvider(QLAggregationKind.SIMPLE),
  Node(QLAggregationKind.SIMPLE),
  Pod(QLAggregationKind.SIMPLE),
  PV(QLAggregationKind.SIMPLE);

  QLAggregationKind aggregationKind;

  QLCCMEntityGroupBy(QLAggregationKind aggregationKind) {
    this.aggregationKind = aggregationKind;
  }

  QLCCMEntityGroupBy() {}

  public QLAggregationKind getAggregationKind() {
    return aggregationKind;
  }
}
