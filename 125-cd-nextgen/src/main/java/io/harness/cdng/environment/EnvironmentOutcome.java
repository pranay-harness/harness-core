package io.harness.cdng.environment;

import io.harness.data.Outcome;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.EnvironmentType;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EnvironmentOutcome implements Outcome {
  String name;
  String identifier;
  String description;
  EnvironmentType type;
  List<NGTag> tags;
}
