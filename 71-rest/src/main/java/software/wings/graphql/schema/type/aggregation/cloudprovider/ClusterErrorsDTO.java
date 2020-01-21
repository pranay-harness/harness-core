package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.ccm.health.CEError;
import lombok.Builder;

import java.util.List;

@Builder
public class ClusterErrorsDTO {
  String clusterId;
  List<CEError> clusterErrors;
}
