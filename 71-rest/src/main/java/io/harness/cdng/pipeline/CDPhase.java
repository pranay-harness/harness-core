package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.persistence.NameAccess;
import io.harness.yaml.core.auxiliary.intfc.ExecutionSection;
import io.harness.yaml.core.auxiliary.intfc.PhaseWrapper;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("phase")
public class CDPhase implements WithIdentifier, NameAccess, PhaseWrapper {
  String identifier;
  String name;
  List<ExecutionSection> steps;
  List<ExecutionSection> rollbackSteps;
}
