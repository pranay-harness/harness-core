package software.wings.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.command.CommandExecutionContext.CodeDeployParams;
import software.wings.sm.StepExecutionSummary;

import java.util.List;

/**
 * Created by rishi on 4/4/17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandStepExecutionSummary extends StepExecutionSummary {
  private String serviceId;
  private String newContainerServiceName;
  private List<ContainerServiceData> newPreviousInstanceCounts;
  private List<ContainerServiceData> oldPreviousInstanceCounts;
  private String clusterName;
  private CodeDeployParams codeDeployParams;
  private CodeDeployParams oldCodeDeployParams;
}
