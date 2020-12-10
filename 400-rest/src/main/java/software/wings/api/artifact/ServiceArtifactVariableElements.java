package software.wings.api.artifact;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("serviceArtifactVariableElements")
public class ServiceArtifactVariableElements implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "serviceArtifactVariableElements";

  List<ServiceArtifactVariableElement> artifactVariableElements;

  @Override
  public String getType() {
    return "serviceArtifactVariableElements";
  }
}
