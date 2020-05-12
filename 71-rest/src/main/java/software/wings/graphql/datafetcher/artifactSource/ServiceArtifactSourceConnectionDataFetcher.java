package software.wings.graphql.datafetcher.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;

import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(CDC)
public class ServiceArtifactSourceConnectionDataFetcher
    extends AbstractArrayDataFetcher<QLArtifactSource, QLArtifactSourceQueryParam> {
  @Inject ArtifactStreamService artifactStreamService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.SERVICE, action = PermissionAttribute.Action.READ)
  protected List<QLArtifactSource> fetch(QLArtifactSourceQueryParam parameters, String accountId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String serviceId = parameters.getServiceId();
      String appId = parameters.getApplicationId();
      List<ArtifactStream> artifactStreams = artifactStreamService.getArtifactStreamsForService(appId, serviceId);
      return artifactStreams.stream()
          .map(ArtifactSourceController::populateArtifactSource)
          .collect(Collectors.toList());
    }
  }

  @Override
  protected QLArtifactSource unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
