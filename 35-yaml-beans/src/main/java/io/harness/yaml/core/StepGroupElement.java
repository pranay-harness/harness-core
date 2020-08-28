package io.harness.yaml.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonTypeName("stepGroup")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepGroupElement implements ExecutionWrapper, WithIdentifier {
  String identifier;
  String name;
  @NotNull List<ExecutionWrapper> steps;
  List<ExecutionWrapper> rollbackSteps;
}
