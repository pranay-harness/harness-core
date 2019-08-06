package software.wings.expression;

import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;

@Builder
@Slf4j
public class DockerConfigFunctor implements ExpressionFunctor {
  private String appId;
  private ArtifactCollectionUtils artifactCollectionUtils;

  private String artifactStreamId;

  @Override
  public String toString() {
    return getDockerConfig();
  }

  private String getDockerConfig() {
    logger.info("Getting dockerConfig");

    try {
      return artifactCollectionUtils.getDockerConfig(artifactStreamId);
    } catch (InvalidRequestException e) {
      logger.error("Error in getDockerConfig", e);
      return "";
    } finally {
      logger.info("Done getting dockerConfig");
    }
  }
}
