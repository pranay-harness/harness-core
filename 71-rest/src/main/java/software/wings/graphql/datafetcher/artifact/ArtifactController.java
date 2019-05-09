package software.wings.graphql.datafetcher.artifact;

import software.wings.beans.artifact.Artifact;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

public class ArtifactController {
  public static void populateArtifact(Artifact artifact, QLArtifactBuilder qlArtifactBuilder) {
    qlArtifactBuilder.id(artifact.getUuid())
        .buildNo(artifact.getBuildNo())
        .collectedAt(GraphQLDateTimeScalar.convert(artifact.getCreatedAt()));
  }
}
