package software.wings.beans.command;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by brett on 11/18/17
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ContainerSetupCommandUnitExecutionData extends CommandExecutionData {
  private String containerServiceName;
  private String kubernetesType;
}
