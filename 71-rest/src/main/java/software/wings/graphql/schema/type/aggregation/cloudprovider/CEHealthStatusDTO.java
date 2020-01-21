package software.wings.graphql.schema.type.aggregation.cloudprovider;

import lombok.Builder;

import java.util.List;

@Builder
public class CEHealthStatusDTO {
  boolean isHealthy;
  List<String> clusterIds;
  List<ClusterErrorsDTO> clusterErrors;
}
