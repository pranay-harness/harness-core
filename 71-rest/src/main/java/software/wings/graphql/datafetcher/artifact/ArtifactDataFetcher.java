package software.wings.graphql.datafetcher.artifact;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import software.wings.beans.artifact.Artifact;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLArtifactQueryParameters;
import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

public class ArtifactDataFetcher extends AbstractDataFetcher<QLArtifact, QLArtifactQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLArtifact fetch(QLArtifactQueryParameters parameters) {
    Artifact artifact = persistence.get(Artifact.class, parameters.getArtifactId());
    if (artifact == null) {
      /**
       * Discussed this with Srinivas, most of the times it is possible that artifact may
       * not be present, so returning null in those cases and not throwing an exception
       */
      return null;
    }

    QLArtifactBuilder qlArtifactBuilder = QLArtifact.builder();
    ArtifactController.populateArtifact(artifact, qlArtifactBuilder);
    return qlArtifactBuilder.build();
  }
}
