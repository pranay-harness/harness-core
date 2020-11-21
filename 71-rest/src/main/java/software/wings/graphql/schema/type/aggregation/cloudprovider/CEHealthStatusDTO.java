package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.ccm.health.CEClusterHealth;

import java.util.List;
import lombok.Builder;

@Builder
public class CEHealthStatusDTO {
  boolean isHealthy;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
