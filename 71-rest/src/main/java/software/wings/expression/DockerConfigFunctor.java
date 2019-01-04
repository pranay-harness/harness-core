package software.wings.expression;

import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import software.wings.helpers.ext.container.ContainerDeploymentManagerHelper;

@Builder
public class DockerConfigFunctor implements ExpressionFunctor {
  private String appId;
  private ContainerDeploymentManagerHelper containerDeploymentHelper;
  private String artifactStreamId;

  @Override
  public String toString() {
    return getDockerConfig();
  }

  private String getDockerConfig() {
    try {
      return containerDeploymentHelper.getDockerConfig(appId, artifactStreamId);
    } catch (InvalidRequestException e) {
      return "";
    }
  }
}
