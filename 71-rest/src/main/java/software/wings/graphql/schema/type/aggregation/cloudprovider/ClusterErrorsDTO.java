package software.wings.graphql.schema.type.aggregation.cloudprovider;

import lombok.Builder;

import java.util.List;

@Builder
public class ClusterErrorsDTO {
  String clusterId;
  List<String> clusterErrors;
}
