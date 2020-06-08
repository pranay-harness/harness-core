package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.ArtifactSourceAttributes;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.artifactsource.ArtifactSource;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.List;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ArtifactSourceType.GCR)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GcrArtifactConfig implements ArtifactConfigWrapper {
  /** GCP connector to connect to Google Container Registry. */
  String gcpConnector;
  /** Registry where the artifact source is located. */
  String registryHostname;
  /** Images in repos need to be referenced via a path. */
  String imagePath;

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
}
