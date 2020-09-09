package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.cdng.visitor.helpers.serviceconfig.DockerHubArtifactConfigVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
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
@JsonTypeName(ArtifactSourceConstants.DOCKER_HUB_NAME)
@SimpleVisitorHelper(helperClass = DockerHubArtifactConfigVisitorHelper.class)
public class DockerHubArtifactConfig implements ArtifactConfig {
  /**
   * Docker hub registry connector.
   */
  @Wither String dockerhubConnector;
  /**
   * Images in repos need to be referenced via a path.
   */
  @Wither String imagePath;
  /**
   * Tag refers to exact tag number.
   */
  @Wither String tag;
  /**
   * Tag regex is used to get latest build from builds matching regex.
   */
  @Wither String tagRegex;
  /**
   * Identifier for artifact.
   */
  String identifier;
  /** Whether this config corresponds to primary artifact.*/
  boolean primaryArtifact;

  @Override
  public ArtifactSourceType getSourceType() {
    return ArtifactSourceType.DOCKER_HUB;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(dockerhubConnector, imagePath);
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    DockerHubArtifactConfig dockerHubArtifactConfig = (DockerHubArtifactConfig) overrideConfig;
    DockerHubArtifactConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(dockerHubArtifactConfig.getDockerhubConnector())) {
      resultantConfig = resultantConfig.withDockerhubConnector(dockerHubArtifactConfig.getDockerhubConnector());
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
