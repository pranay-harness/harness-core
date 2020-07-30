package software.wings.beans.command;

import io.harness.k8s.model.ImageDetails;
import lombok.Data;
import software.wings.beans.container.ContainerTask;

@Data
public class ContainerSetupParams {
  private String serviceName;
  private String clusterName;
  private String appName;
  private String envName;
  private ImageDetails imageDetails;
  private ContainerTask containerTask;
  private String infraMappingId;
  private int serviceSteadyStateTimeout;
}
