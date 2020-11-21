package software.wings.api.artifact;

import io.harness.data.SweepingOutput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceArtifactVariableElements implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "serviceArtifactVariableElements";

  List<ServiceArtifactVariableElement> artifactVariableElements;
}
