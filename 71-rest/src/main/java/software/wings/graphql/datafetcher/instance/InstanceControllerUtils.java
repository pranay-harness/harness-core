package software.wings.graphql.datafetcher.instance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.artifact.ArtifactController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.artifact.QLArtifact;
import software.wings.graphql.schema.type.artifact.QLArtifact.QLArtifactBuilder;

@Singleton
public class InstanceControllerUtils {
  @Inject HPersistence persistence;

  public QLArtifact getQlArtifact(Instance instance) {
    Artifact artifact = persistence.get(Artifact.class, instance.getLastArtifactId());
    if (artifact == null) {
      return QLArtifact.builder()
          .buildNo(instance.getLastArtifactBuildNum())
          .id(instance.getLastArtifactId())
          .collectedAt(GraphQLDateTimeScalar.convert(instance.getLastDeployedAt()))
          .build();
    }
    QLArtifactBuilder qlArtifactBuilder = QLArtifact.builder();
    ArtifactController.populateArtifact(artifact, qlArtifactBuilder);
    return qlArtifactBuilder.build();
  }
}
