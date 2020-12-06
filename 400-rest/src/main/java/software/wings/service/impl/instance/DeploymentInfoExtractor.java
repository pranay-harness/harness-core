package software.wings.service.impl.instance;

import software.wings.api.DeploymentInfo;

import java.util.List;
import java.util.Optional;

public interface DeploymentInfoExtractor {
  Optional<List<DeploymentInfo>> extractDeploymentInfo();
}
