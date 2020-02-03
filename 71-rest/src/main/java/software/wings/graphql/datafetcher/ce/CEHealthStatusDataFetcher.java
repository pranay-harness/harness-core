package software.wings.graphql.datafetcher.ce;

import com.google.inject.Inject;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import software.wings.graphql.schema.type.aggregation.cloudprovider.CEHealthStatusDTO;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;

public class CEHealthStatusDataFetcher implements DataFetcher<CEHealthStatusDTO> {
  @Inject HealthStatusService healthStatusService;

  public DataFetcher get() {
    return dataFetchingEnvironment -> get(dataFetchingEnvironment);
  }

  @Override
  public CEHealthStatusDTO get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    QLKubernetesClusterCloudProvider cloudProvider = dataFetchingEnvironment.getSource();
    try {
      CEHealthStatus ceHealthStatus = healthStatusService.getHealthStatus(cloudProvider.getId());
      return CEHealthStatusDTO.builder()
          .isHealthy(ceHealthStatus.isHealthy())
          .clusterHealthStatusList(ceHealthStatus.getCeClusterHealthList())
          .build();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}