package software.wings.helpers.ext.nexus;

import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.common.AlphanumComparator;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@Slf4j
public class NexusHelper {
  @NotNull
  public List<BuildDetails> constructBuildDetails(String repoId, String groupId, String artifactName,
      List<String> versions, Map<String, String> versionToArtifactUrls) {
    logger.info("Versions come from nexus server {}", versions);
    versions = versions.stream().sorted(new AlphanumComparator()).collect(toList());
    logger.info("After sorting alphanumerically versions {}", versions);

    return versions.stream()
        .map(version -> {
          Map<String, String> metadata = new HashMap<>();
          metadata.put(ArtifactMetadataKeys.repositoryName, repoId);
          metadata.put(ArtifactMetadataKeys.nexusGroupId, groupId);
          metadata.put(ArtifactMetadataKeys.nexusArtifactId, artifactName);
          metadata.put(ArtifactMetadataKeys.version, version);
          return aBuildDetails()
              .withNumber(version)
              .withRevision(version)
              .withBuildUrl(versionToArtifactUrls.get(version))
              .withMetadata(metadata)
              .withUiDisplayName("Version# " + version)
              .build();
        })
        .collect(toList());
  }
}
