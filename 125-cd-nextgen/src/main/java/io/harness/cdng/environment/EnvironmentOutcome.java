package io.harness.cdng.environment;

import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("environmentOutcome")
public class EnvironmentOutcome implements Outcome, SweepingOutput {
  String name;
  String identifier;
  String description;
  EnvironmentType environmentType;
  List<NGTag> tags;

  @Override
  public String getType() {
    return "environmentOutcome";
  }
}
