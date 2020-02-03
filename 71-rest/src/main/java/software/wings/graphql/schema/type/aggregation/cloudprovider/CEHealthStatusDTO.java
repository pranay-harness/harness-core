package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.ccm.health.CEClusterHealth;
import lombok.Builder;

import java.util.List;

@Builder
public class CEHealthStatusDTO {
  boolean isHealthy;
  List<CEClusterHealth> clusterHealthStatusList;
}
