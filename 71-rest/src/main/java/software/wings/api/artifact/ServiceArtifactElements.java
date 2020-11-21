package software.wings.api.artifact;

import io.harness.data.SweepingOutput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceArtifactElements implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "serviceArtifactElements";

  List<ServiceArtifactElement> artifactElements;
}
