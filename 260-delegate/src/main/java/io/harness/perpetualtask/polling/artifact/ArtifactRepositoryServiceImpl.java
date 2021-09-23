package io.harness.perpetualtask.polling.artifact;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactTaskType;
import io.harness.delegate.task.artifacts.DelegateArtifactTaskHandler;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskExecutionResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.CDC)
@Singleton
public class ArtifactRepositoryServiceImpl {
  @Inject ArtifactCollectionServiceRegistryNg serviceRegistry;

  public ArtifactTaskExecutionResponse collectBuilds(ArtifactTaskParameters params) {
    ArtifactSourceDelegateRequest attributes = params.getAttributes();
    DelegateArtifactTaskHandler handler = serviceRegistry.getBuildService(attributes.getSourceType());
    handler.decryptRequestDTOs(params.getAttributes());
    if (params.getArtifactTaskType().equals(ArtifactTaskType.GET_BUILDS)) {
      return handler.getBuilds(attributes);
    }
    return null;
  }
}
