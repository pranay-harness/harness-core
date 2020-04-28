package software.wings.graphql.datafetcher.artifactSource;

import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLArtifactSourceQueryParam;
import software.wings.graphql.schema.type.artifactSource.QLArtifactSource;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;

public class ArtifactSourceConnectionDataFetcher
    extends AbstractObjectDataFetcher<QLArtifactSource, QLArtifactSourceQueryParam> {
  @Inject ArtifactStreamService artifactStreamService;
  @Inject ArtifactService artifactService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLArtifactSource fetch(QLArtifactSourceQueryParam parameters, String accountId) {
    String artifactId = parameters.getArtifactId();
    Artifact artifact = artifactService.get(accountId, artifactId);
    notNullCheck("Couldn't find artifact for ID: " + artifactId, artifact);
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getArtifactStreamId());
    return ArtifactSourceController.populateArtifactSource(artifactStream);
  }
}
