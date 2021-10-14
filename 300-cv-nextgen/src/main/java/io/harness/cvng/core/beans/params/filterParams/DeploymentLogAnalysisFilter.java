package io.harness.cvng.core.beans.params.filterParams;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;

import java.util.List;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Value
@NoArgsConstructor
public class DeploymentLogAnalysisFilter extends LogAnalysisFilter {
  @QueryParam("clusterTypes") List<ClusterType> clusterTypes;
  @QueryParam("hostName") String hostName;

  public boolean filterByHostName() {
    return isNotEmpty(hostName);
  }

  public boolean filterByClusterType() {
    return isNotEmpty(clusterTypes);
  }
}
