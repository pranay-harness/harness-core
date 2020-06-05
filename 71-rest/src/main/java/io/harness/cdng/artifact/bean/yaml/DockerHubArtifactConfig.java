package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.artifactsource.DockerArtifactSourceAttributes;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Value;

import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(ArtifactSourceType.DOCKER_HUB)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerHubArtifactConfig implements ArtifactConfig {
  @NotNull String identifier;
  @Builder.Default String sourceType = ArtifactSourceType.DOCKER_HUB;
  @NotNull DockerSpec spec;

  @Data
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class DockerSpec implements Spec {
    /** Docker hub registry connector. */
    String dockerhubConnector;
    /** Images in repos need to be referenced via a path. */
    String imagePath;
    /** Tag refers to exact tag number. */
    String tag;
    /** Tag regex is used to get latest build from builds matching regex. */
    String tagRegex;

    public String getUniqueHash() {
      List<String> valuesList = Arrays.asList(dockerhubConnector, imagePath, getTag(), getTagRegex());
      return ArtifactUtils.generateUniqueHashFromStringList(valuesList);
    }

    @Override
    public DockerArtifactSourceAttributes getSourceAttributes() {
      return DockerArtifactSourceAttributes.newBuilder()
          .dockerhubConnector(dockerhubConnector)
          .imagePath(imagePath)
          .tag(tag)
          .tagRegex(tagRegex)
          .build();
    }
  }
}
