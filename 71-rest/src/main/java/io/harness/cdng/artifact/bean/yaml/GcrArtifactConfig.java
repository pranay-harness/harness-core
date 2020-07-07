package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
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
public class GcrArtifactConfig implements ArtifactConfigWrapper {
  /** GCP connector to connect to Google Container Registry. */
  @Wither String gcpConnector;
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
    List<String> valuesList = Arrays.asList(gcpConnector, registryHostname, imagePath);
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
  public String setArtifactType(String artifactType) {
    this.artifactType = artifactType;
    return artifactType;
  }

  @Override
  public String setIdentifier(String identifier) {
    this.identifier = identifier;
    return identifier;
  }

  @Override
  public ArtifactConfigWrapper applyOverrides(ArtifactConfigWrapper overrideConfig) {
    GcrArtifactConfig gcrArtifactConfig = (GcrArtifactConfig) overrideConfig;
    GcrArtifactConfig resultantConfig = this;
    if (EmptyPredicate.isNotEmpty(gcrArtifactConfig.getGcpConnector())) {
      resultantConfig = resultantConfig.withGcpConnector(gcrArtifactConfig.getGcpConnector());
    }
    if (EmptyPredicate.isNotEmpty(gcrArtifactConfig.getImagePath())) {
      resultantConfig = resultantConfig.withImagePath(gcrArtifactConfig.getImagePath());
    }
    if (EmptyPredicate.isNotEmpty(gcrArtifactConfig.getRegistryHostname())) {
      resultantConfig = resultantConfig.withRegistryHostname(gcrArtifactConfig.getRegistryHostname());
    }
    return resultantConfig;
  }
}
