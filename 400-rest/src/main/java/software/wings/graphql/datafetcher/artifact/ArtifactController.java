package software.wings.graphql.datafetcher.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.Artifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class ArtifactController {
  public static void populateArtifact(Artifact artifact, QLArtifactBuilder qlArtifactBuilder) {
    qlArtifactBuilder.id(artifact.getUuid())
        .buildNo(artifact.getBuildNo())
        .collectedAt(artifact.getCreatedAt())
        .artifactSourceId(artifact.getArtifactStreamId());
  }
}
