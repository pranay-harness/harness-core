package io.harness.cdng.environment.yaml;

import io.harness.cdng.common.beans.Tag;
import io.harness.cdng.environment.beans.EnvironmentType;
import io.harness.data.Outcome;
import io.harness.state.io.StepParameters;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.List;

@Value
@Builder
public class EnvironmentYaml implements StepParameters, Outcome {
  @NonFinal private String displayName;
  private String identifier;
  private EnvironmentType type;
  private List<Tag> tags;

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
