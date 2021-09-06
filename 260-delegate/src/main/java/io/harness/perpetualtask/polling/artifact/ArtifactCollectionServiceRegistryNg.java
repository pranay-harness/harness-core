package io.harness.perpetualtask.polling.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.docker.DockerArtifactTaskHandler;
import io.harness.delegate.task.artifacts.ecr.EcrArtifactTaskHandler;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactTaskHandler;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ArtifactCollectionServiceRegistryNg {
  @Inject Injector injector;

  public DelegateArtifactTaskHandler getBuildService(ArtifactSourceType artifactSourceType) {
    Class<? extends DelegateArtifactTaskHandler> buildServiceClass = getBuildServiceClass(artifactSourceType);
    return injector.getInstance(Key.get(buildServiceClass));
  }

  public Class<? extends DelegateArtifactTaskHandler> getBuildServiceClass(ArtifactSourceType artifactSourceType) {
    switch (artifactSourceType) {
      case DOCKER_REGISTRY:
        return DockerArtifactTaskHandler.class;
      case ECR:
        return EcrArtifactTaskHandler.class;
      case GCR:
        return GcrArtifactTaskHandler.class;
      default:
        throw new InvalidRequestException("Unknown artifact source type: " + artifactSourceType);
    }
  }
}
