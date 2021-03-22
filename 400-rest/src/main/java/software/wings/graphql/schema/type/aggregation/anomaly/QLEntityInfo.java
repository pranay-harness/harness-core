package software.wings.graphql.schema.type.aggregation.anomaly;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLObject;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEntityInfo implements QLObject {
  String clusterName;
  String clusterId;
  String namespace;
  String workloadName;
  String workloadType;
  String gcpProject;
  String gcpProduct;
  String gcpSKUId;
  String gcpSKUDescription;
  String awsAccount;
  String awsService;
}
