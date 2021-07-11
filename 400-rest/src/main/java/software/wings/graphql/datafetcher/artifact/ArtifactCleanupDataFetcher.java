package software.wings.graphql.datafetcher.artifact;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.workers.background.iterator.ArtifactCleanupHandler;

import software.wings.beans.User;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamType;
import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.mutation.artifact.ArtifactCleanUpPayload;
import software.wings.graphql.schema.mutation.artifact.ArtifactCleanupInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.UserThreadLocal;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.artifact.ArtifactCleanupServiceSyncImpl;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.AuthService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactCleanupDataFetcher extends BaseMutatorDataFetcher<ArtifactCleanupInput, ArtifactCleanUpPayload> {
  private ArtifactCleanupServiceSyncImpl artifactCleanupService;
  private ArtifactCleanupHandler artifactCleanupHandler;
  private AuthService authService;
  private ArtifactStreamService artifactStreamService;

  @Inject
  public ArtifactCleanupDataFetcher(ArtifactCleanupServiceSyncImpl artifactCleanupService,
      ArtifactStreamService artifactStreamService, ArtifactCleanupHandler artifactCleanupHandler,
      AuthService authService) {
    super(ArtifactCleanupInput.class, ArtifactCleanUpPayload.class);
    this.artifactCleanupService = artifactCleanupService;
    this.artifactCleanupHandler = artifactCleanupHandler;
    this.authService = authService;
    this.artifactStreamService = artifactStreamService;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.SERVICE)
  protected ArtifactCleanUpPayload mutateAndFetch(ArtifactCleanupInput parameter, MutationContext mutationContext) {
    ArtifactStream artifactStream = artifactStreamService.get(parameter.getArtifactStreamId());

    if (artifactStream == null) {
      return new ArtifactCleanUpPayload("Artifact stream not found for the id: " + parameter.getArtifactStreamId());
    }

    if (!ImmutableSet
             .of(ArtifactStreamType.DOCKER.name(), ArtifactStreamType.AMI.name(), ArtifactStreamType.ARTIFACTORY.name(),
                 ArtifactStreamType.ECR.name(), ArtifactStreamType.GCR.name(), ArtifactStreamType.ACR.name(),
                 ArtifactStreamType.NEXUS.name(), ArtifactStreamType.AZURE_MACHINE_IMAGE.name())
             .contains(artifactStream.getArtifactStreamType())) {
      return new ArtifactCleanUpPayload(
          "Clean up not supported for artifact Stream type: " + artifactStream.getArtifactStreamType());
    }

    String accountId = artifactStream.getAccountId();
    if (accountId == null) {
      accountId = artifactCleanupHandler.fetchAccountId(artifactStream);
    }

    final User user = UserThreadLocal.get();
    if (user != null) {
      authService.authorize(accountId, artifactStream.getAppId(), artifactStream.getServiceId(), user,
          asList(new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                     PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.READ),
              new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                  PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.CREATE),
              new PermissionAttribute(PermissionAttribute.ResourceType.SERVICE,
                  PermissionAttribute.PermissionType.SERVICE, PermissionAttribute.Action.UPDATE)),
          true);
    }

    artifactCleanupHandler.handleManually(artifactStream, accountId);

    return new ArtifactCleanUpPayload(
        "Cleanup successful for Artifact stream with id: " + parameter.getArtifactStreamId());
  }
}
