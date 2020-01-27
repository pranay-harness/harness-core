package software.wings.graphql.datafetcher.ce;

import com.google.inject.Inject;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.health.CEHealthStatus;
import io.harness.ccm.health.HealthStatusService;
import software.wings.graphql.schema.type.aggregation.cloudprovider.CEHealthStatusDTO;
import software.wings.graphql.schema.type.aggregation.cloudprovider.ClusterErrorsDTO;
import software.wings.graphql.schema.type.cloudProvider.QLKubernetesClusterCloudProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
          .clusterIds(ceHealthStatus.getClusterIds())
          .clusterErrors(from(ceHealthStatus.getClusterErrors()))
          .build();
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static List<ClusterErrorsDTO> from(Map<String, List<String>> clusterErrorMap) {
    List<ClusterErrorsDTO> clusterErrors = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : clusterErrorMap.entrySet()) {
      clusterErrors.add(ClusterErrorsDTO.builder().clusterId(entry.getKey()).clusterErrors(entry.getValue()).build());
    }
    return clusterErrors;
  }
}