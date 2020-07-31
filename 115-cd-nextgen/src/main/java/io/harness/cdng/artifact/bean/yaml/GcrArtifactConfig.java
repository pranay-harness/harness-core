package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.ArtifactOutcome;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.delegate.beans.ArtifactAttributes;
import io.harness.cdng.artifact.delegate.beans.ArtifactSourceAttributes;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.structure.EmptyPredicate;
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
@JsonTypeName(ArtifactSourceType.GCR)
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
  /** Type to identify whether primary and sidecars artifact. */
  String artifactType;

  @Override
  public String getSourceType() {
    return ArtifactSourceType.GCR;
  }

  @Override
  public String getUniqueHash() {
    List<String> valuesList = Arrays.asList(gcrConnector, registryHostname, imagePath);
    return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
  }

  @Override
  public ArtifactSource getArtifactSource(String accountId) {
    return null;
  }

  @Override
  public ArtifactSourceAttributes getSourceAttributes() {
    return null;
  }

  @Override
  public ArtifactOutcome getArtifactOutcome(ArtifactAttributes artifactAttributes) {
    return null;
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
