package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.List;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ArtifactSourceType.DOCKER_HUB)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerHubArtifactConfig implements ArtifactConfigWrapper {
  /** Docker hub registry connector. */
  String dockerhubConnector;
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
  /** Identifier for artifact. */
  String identifier;
  /** Type to identify whether primary and sidecars artifact. */
  String artifactType;

  @Override
  public String getSourceType() {
    return ArtifactSourceType.DOCKER_HUB;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(dockerhubConnector, imagePath);
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactSource getArtifactSource(String accountId) {
    return DockerArtifactSource.builder()
        .accountId(accountId)
        .sourceType(getSourceType())
        .dockerHubConnector(dockerhubConnector)
        .imagePath(imagePath)
        .uniqueHash(getUniqueHash())
        .build();
  }

  @Override
  public ArtifactSourceAttributes getSourceAttributes() {
    return DockerArtifactSourceAttributes.builder()
        .dockerhubConnector(dockerhubConnector)
        .imagePath(imagePath)
        .tag(tag)
        .tagRegex(tagRegex)
        .build();
  }

  @Override
  public String setArtifactType(String artifactType) {
    this.artifactType = artifactType;
    return artifactType;
  }

  @Override
  public String setIdentifier(String identifier) {
    this.identifier = identifier;
    return identifier;
  }
}
