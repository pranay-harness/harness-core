package software.wings.cloudprovider;

import io.harness.logging.CommandExecutionStatus;

import com.amazonaws.services.ec2.model.Instance;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

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
