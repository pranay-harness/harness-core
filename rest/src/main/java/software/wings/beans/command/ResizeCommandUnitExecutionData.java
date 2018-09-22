package software.wings.beans.command;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.api.ContainerServiceData;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

/**
 * Created by anubhaw on 2/28/17.
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ResizeCommandUnitExecutionData extends CommandExecutionData {
  private List<ContainerInfo> containerInfos;
  private List<ContainerServiceData> newInstanceData;
  private List<ContainerServiceData> oldInstanceData;
}
