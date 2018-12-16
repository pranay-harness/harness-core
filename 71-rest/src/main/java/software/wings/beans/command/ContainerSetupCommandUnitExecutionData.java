package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.Label;

import java.util.List;

/**
 * Created by brett on 11/18/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ContainerSetupCommandUnitExecutionData extends CommandExecutionData {
  private String containerServiceName;
  private String namespace;
  private List<String[]> activeServiceCounts;
  private List<String[]> trafficWeights;
  private String autoscalerYaml;
  // Following 3 fields are required while Daemon ECS service rollback
  private String previousEcsServiceSnapshotJson;
  private String ecsServiceArn;
  private String ecsTaskDefintion;
  private List<Label> lookupLabels;
  private List<AwsAutoScalarConfig> previousAwsAutoScalarConfigs;
  private String loadBalancer;
}
