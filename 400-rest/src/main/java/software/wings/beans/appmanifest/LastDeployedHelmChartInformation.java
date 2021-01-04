package software.wings.beans.appmanifest;

import io.harness.beans.WorkflowType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LastDeployedHelmChartInformation {
  HelmChart helmchart;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
