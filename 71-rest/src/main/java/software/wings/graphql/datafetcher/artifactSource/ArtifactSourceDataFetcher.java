package software.wings.graphql.datafetcher.artifactSource;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactStreamService;

@OwnedBy(CDC)
public class ArtifactSourceDataFetcher extends AbstractObjectDataFetcher<QLArtifactSource, QLArtifactSourceQueryParam> {
  @Inject ArtifactStreamService artifactStreamService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLArtifactSource fetch(QLArtifactSourceQueryParam parameters, String accountId) {
    String artifactSourceId = parameters.getArtifactSourceId();

    if (artifactSourceId != null) {
      ArtifactStream artifactStream = artifactStreamService.get(artifactSourceId);
      return ArtifactSourceController.populateArtifactSource(artifactStream);
    }

    return null;
  }
}
