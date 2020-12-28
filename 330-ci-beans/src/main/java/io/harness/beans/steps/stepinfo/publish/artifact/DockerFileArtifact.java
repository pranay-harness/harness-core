package io.harness.beans.steps.stepinfo.publish.artifact;

import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.WithImageConnector;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonDeserialize
@TypeAlias("dockerFileArtifact")
public class DockerFileArtifact implements Artifact, WithImageConnector {
  @NotNull private ParameterField<String> dockerFile;
  @NotNull private ParameterField<String> context;
  @NotNull private ParameterField<String> image;
  @NotNull private ParameterField<String> tag;
  private List<BuildArgument> buildArguments;
  @NotNull private ArtifactConnector connector;

  @Override
  public Type getType() {
    return Type.DOCKER_FILE;
  }

  @Override
  public void setConnector(ArtifactConnector connector) {
    this.connector = connector;
  }

  @Value
  @Builder
  public static class BuildArgument {
    @NotNull private ParameterField<String> key;
    @NotNull private ParameterField<String> value;
  }
}
