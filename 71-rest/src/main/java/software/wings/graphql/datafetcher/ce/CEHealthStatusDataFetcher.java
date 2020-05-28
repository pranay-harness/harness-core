package software.wings.graphql.datafetcher.ce;

import com.google.inject.Inject;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import software.wings.graphql.schema.type.aggregation.cloudprovider.CEHealthStatusDTO;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;

public class CEHealthStatusDataFetcher implements DataFetcher<CEHealthStatusDTO> {
  @Inject HealthStatusService healthStatusService;

  public DataFetcher get() {
    return dataFetchingEnvironment -> get(dataFetchingEnvironment);
  }

  @Override
  public CEHealthStatusDTO get(DataFetchingEnvironment dataFetchingEnvironment) throws Exception {
    QLCloudProvider cloudProvider = dataFetchingEnvironment.getSource();
    String cloudProviderId = cloudProvider.getId();

    try {
      CEHealthStatus ceHealthStatus = healthStatusService.getHealthStatus(cloudProviderId, false);
      return CEHealthStatusDTO.builder()
          .isHealthy(ceHealthStatus.isHealthy())
          .messages(ceHealthStatus.getMessages())
          .clusterHealthStatusList(ceHealthStatus.getCeClusterHealthList())
          .build();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}