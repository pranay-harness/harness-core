package software.wings.graphql.schema.type.aggregation.cloudprovider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.health.CEClusterHealth;

import java.util.List;
import lombok.Builder;

@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class CEHealthStatusDTO {
  boolean isHealthy;
  List<String> messages;
  List<CEClusterHealth> clusterHealthStatusList;
}
