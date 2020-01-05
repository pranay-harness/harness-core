package software.wings.graphql.datafetcher.artifact;

import lombok.experimental.UtilityClass;
import software.wings.beans.artifact.Artifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

@UtilityClass
public class ArtifactController {
  public static void populateArtifact(Artifact artifact, QLArtifactBuilder qlArtifactBuilder) {
    qlArtifactBuilder.id(artifact.getUuid()).buildNo(artifact.getBuildNo()).collectedAt(artifact.getCreatedAt());
  }
}
