package software.wings.graphql.datafetcher.instance;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HPersistence;

import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.artifact.ArtifactController;
import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(Module._380_CG_GRAPHQL)
public class InstanceControllerUtils {
  public static final String DUMMY_ARTIFACT_SOURCE_ID = "DUMMY_ARTIFACT_SOURCE_ID";
  @Inject HPersistence persistence;

  public QLArtifact getQlArtifact(Instance instance) {
    Artifact artifact = persistence.get(Artifact.class, instance.getLastArtifactId());
    if (artifact == null) {
      return QLArtifact.builder()
          .buildNo(instance.getLastArtifactBuildNum())
          .artifactSourceId(EmptyPredicate.isNotEmpty(instance.getLastArtifactStreamId())
                  ? instance.getLastArtifactStreamId()
                  : DUMMY_ARTIFACT_SOURCE_ID)
          .id(instance.getLastArtifactId())
          .collectedAt(instance.getLastDeployedAt())
          .build();
    }
    QLArtifactBuilder qlArtifactBuilder = QLArtifact.builder();
    ArtifactController.populateArtifact(artifact, qlArtifactBuilder);
    return qlArtifactBuilder.build();
  }
}
