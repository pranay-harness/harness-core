package io.harness.cdng.environment;

import io.harness.data.Outcome;
import io.harness.ng.core.common.beans.Tag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class EnvironmentOutcome implements Outcome {
  String name;
  String identifier;
  EnvironmentType type;
  List<Tag> tags;
}
