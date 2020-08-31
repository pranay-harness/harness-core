package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.artifacts.ArtifactSourceConstants;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
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
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ArtifactSourceConstants.GCR_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcrArtifactConfig implements ArtifactConfig {
  /** GCP connector to connect to Google Container Registry. */
  @Wither String gcrConnector;
  /** Registry where the artifact source is located. */
  @Wither String registryHostname;
  /** Images in repos need to be referenced via a path. */
  @Wither String imagePath;
  /** Identifier for artifact. */
  String identifier;
  /** Whether this config corresponds to primary artifact.*/
  boolean isPrimaryArtifact;

  @Override
  public ArtifactSourceType getSourceType() {
    return ArtifactSourceType.GCR;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(gcrConnector, registryHostname, imagePath);
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactConfig applyOverrides(ArtifactConfig overrideConfig) {
    GcrArtifactConfig gcrArtifactSpecConfig = (GcrArtifactConfig) overrideConfig;
    GcrArtifactConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(gcrArtifactSpecConfig.getGcrConnector())) {
      resultantConfig = resultantConfig.withGcrConnector(gcrArtifactSpecConfig.getGcrConnector());
    }
    if (EmptyPredicate.isNotEmpty(gcrArtifactSpecConfig.getImagePath())) {
      resultantConfig = resultantConfig.withImagePath(gcrArtifactSpecConfig.getImagePath());
    }
    if (EmptyPredicate.isNotEmpty(gcrArtifactSpecConfig.getRegistryHostname())) {
      resultantConfig = resultantConfig.withRegistryHostname(gcrArtifactSpecConfig.getRegistryHostname());
    }
    return resultantConfig;
  }
}
