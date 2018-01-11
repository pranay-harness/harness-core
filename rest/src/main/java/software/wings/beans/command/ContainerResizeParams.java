package software.wings.beans.command;

import lombok.Data;
import software.wings.api.ContainerServiceData;

import java.util.ArrayList;
import java.util.List;

@Data
public class ContainerResizeParams {
  private String clusterName;
  private List<ContainerServiceData> desiredCounts = new ArrayList<>();
  private int serviceSteadyStateTimeout;
}
