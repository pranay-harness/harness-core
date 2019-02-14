package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.command.CommandExecutionData;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.beans.container.Label;

import java.util.List;

/**
 * Created by brett on 11/18/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ContainerSetupCommandUnitExecutionData implements CommandExecutionData {
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

  // Only to be used by ECS BG
  private boolean ecsBlueGreen;
  private String prodEcsListener;
  private String stageEcsListener;

  // This is Target Group associated with Service
  private String targetGroupForNewService;
  private String targetGroupForExistingService;
  private String ecsRegion;
  private String ecsServiceToBeDownsized;
  private int countToBeDownsizedForOldService;

  // For ECS BG Route 53 DNS weight swap
  private boolean useRoute53Swap;
  private String parentRecordName;
  private String parentRecordHostedZoneId;
  private String oldServiceDiscoveryArn;
  private String newServiceDiscoveryArn;
}