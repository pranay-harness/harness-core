package software.wings.graphql.schema.type.aggregation.anomaly;

import software.wings.graphql.schema.type.QLObject;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
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
