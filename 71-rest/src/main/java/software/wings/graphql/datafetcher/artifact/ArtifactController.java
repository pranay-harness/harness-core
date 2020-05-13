package software.wings.graphql.datafetcher.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.experimental.UtilityClass;
import software.wings.beans.artifact.Artifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

@OwnedBy(CDC)
@UtilityClass
public class ArtifactController {
  public static void populateArtifact(Artifact artifact, QLArtifactBuilder qlArtifactBuilder) {
    qlArtifactBuilder.id(artifact.getUuid())
        .buildNo(artifact.getBuildNo())
        .collectedAt(artifact.getCreatedAt())
        .artifactSourceId(artifact.getArtifactStreamId());
  }
}
