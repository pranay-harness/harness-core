package software.wings.service.impl.instance.sync.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;

import java.util.Collection;

@Data
@Builder
public class ContainerFilter {
  private Collection<ContainerDeploymentInfo> containerDeploymentInfoCollection;
}
