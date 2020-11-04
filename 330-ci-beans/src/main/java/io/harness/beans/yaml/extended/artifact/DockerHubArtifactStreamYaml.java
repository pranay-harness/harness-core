package io.harness.beans.yaml.extended.artifact;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.intfc.ArtifactStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("docker-hub")
public class DockerHubArtifactStreamYaml implements ArtifactStream {
  @NotNull private String type;
  @NotNull private Spec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class Spec {
    @NotNull private String dockerhubConnector;
    @NotNull private String imagePath;

    @NotNull private String tag;
  }
}
