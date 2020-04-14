package software.wings.api.artifact;

import io.harness.beans.SweepingOutput;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ServiceArtifactVariableElements implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "serviceArtifactVariableElements";

  List<ServiceArtifactVariableElement> artifactVariableElements;
}
