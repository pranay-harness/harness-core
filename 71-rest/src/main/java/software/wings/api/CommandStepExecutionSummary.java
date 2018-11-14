package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CodeDeployParams;
import software.wings.beans.container.AwsAutoScalarConfig;
import software.wings.sm.StepExecutionSummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rishi on 4/4/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandStepExecutionSummary extends StepExecutionSummary {
  private String serviceId;
  private List<ContainerServiceData> newInstanceData = new ArrayList<>();
  private List<ContainerServiceData> oldInstanceData = new ArrayList<>();
  private String controllerNamePrefix;
  // Following 3 fields are required while Daemon ECS service rollback
  private String previousEcsServiceSnapshotJson;
  private String ecsServiceArn;
  private String ecsTaskDefintion;
  private List<AwsAutoScalarConfig> previousAwsAutoScalarConfigs;
  private String clusterName;
  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;
  private String codeDeployDeploymentId;
}
