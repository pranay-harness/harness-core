package software.wings.service.impl.instance.sync.request;

import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;

import java.util.Collection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerFilter {
  private Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection;
}
