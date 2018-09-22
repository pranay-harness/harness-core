package software.wings.cloudprovider;

import com.amazonaws.services.ec2.model.Instance;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

import java.util.List;

/**
 * Created by anubhaw on 6/23/17.
 */
@Data
@NoArgsConstructor
public class CodeDeployDeploymentInfo {
  private CommandExecutionStatus status;
  private List<Instance> instances;
  private String deploymentId;
}
