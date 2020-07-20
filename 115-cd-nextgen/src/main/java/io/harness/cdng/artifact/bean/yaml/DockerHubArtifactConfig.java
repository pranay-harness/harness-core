package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSource;
import io.harness.cdng.artifact.delegate.beans.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.ArtifactSourceAttributes;
import io.harness.cdng.artifact.delegate.beans.DockerArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.structure.EmptyPredicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Wither;

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
public class DockerHubArtifactConfig implements ArtifactConfig {
  /** Docker hub registry connector. */
  @Wither String connectorIdentifier;
  /** Images in repos need to be referenced via a path. */
  @Wither String imagePath;
  /** Tag refers to exact tag number. */
  @Wither String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  @Wither String tagRegex;
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
    List<String> valuesList = Arrays.asList(connectorIdentifier, imagePath);
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactSource getArtifactSource(String accountId) {
    return DockerArtifactSource.builder()
        .accountId(accountId)
        .sourceType(getSourceType())
        .dockerHubConnector(connectorIdentifier)
        .imagePath(imagePath)
        .uniqueHash(getUniqueHash())
        .build();
  }

  @Override
  public ArtifactSourceAttributes getSourceAttributes() {
    // If both are empty, regex is latest among all docker artifacts.
    if (EmptyPredicate.isEmpty(tag) && EmptyPredicate.isEmpty(tagRegex)) {
      tagRegex = "*";
    }
    return DockerArtifactSourceAttributes.builder()
        .dockerhubConnector(connectorIdentifier)
        .imagePath(imagePath)
        .tag(tag)
        .tagRegex(tagRegex)
        .build();
  }

  @Override
  public ArtifactOutcome getArtifactOutcome(ArtifactAttributes artifactAttributes) {
    DockerArtifactAttributes dockerArtifactAttributes = (DockerArtifactAttributes) artifactAttributes;
    return DockerArtifactOutcome.builder()
        .dockerhubConnector(dockerArtifactAttributes.getDockerHubConnector())
        .imagePath(dockerArtifactAttributes.getImagePath())
        .tag(dockerArtifactAttributes.getTag())
        .tagRegex(tagRegex)
        .identifier(identifier)
        .artifactType(artifactType)
        .build();
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) overrideConfig;
    DockerHubArtifactConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(dockerHubArtifactConfig.getConnectorIdentifier())) {
      resultantConfig = resultantConfig.withConnectorIdentifier(dockerHubArtifactConfig.getConnectorIdentifier());
    }
    if (EmptyPredicate.isNotEmpty(dockerHubArtifactConfig.getImagePath())) {
      resultantConfig = resultantConfig.withImagePath(dockerHubArtifactConfig.getImagePath());
    }
    if (EmptyPredicate.isNotEmpty(dockerHubArtifactConfig.getTag())) {
      resultantConfig = resultantConfig.withTag(dockerHubArtifactConfig.getTag());
    }
    if (EmptyPredicate.isNotEmpty(dockerHubArtifactConfig.getTagRegex())) {
      resultantConfig = resultantConfig.withTagRegex(dockerHubArtifactConfig.getTagRegex());
    }
    return resultantConfig;
  }
}
