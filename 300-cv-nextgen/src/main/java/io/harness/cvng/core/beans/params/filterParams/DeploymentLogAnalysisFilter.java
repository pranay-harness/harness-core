package io.harness.cvng.core.beans.params.filterParams;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.analysis.beans.DeploymentLogAnalysisDTO.ClusterType;

import java.util.List;
import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@SuperBuilder
@Value
public class DeploymentLogAnalysisFilter extends LogAnalysisFilter {
  List<ClusterType> clusterTypes;
  String hostName;

  public boolean filterByHostName() {
    return isNotEmpty(hostName);
  }

  public boolean filterByClusterType() {
    return isNotEmpty(clusterTypes);
  }
}
