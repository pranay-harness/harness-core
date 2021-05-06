package io.harness.plancreator.steps;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.plancreator.execution.ExecutionWrapperConfig;
import io.harness.yaml.core.serializer.ParallelStepElementSerializer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

/**
 * Parallel structure is special list of steps that can be executed in parallel.
 */
@Data
@Builder
@NoArgsConstructor
@JsonTypeName("parallel")
@JsonSerialize(using = ParallelStepElementSerializer.class)
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
@TypeAlias("io.harness.yaml.core.parallelStepElementConfig")
public class ParallelStepElementConfig {
  @NotNull List<ExecutionWrapperConfig> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStepElementConfig(List<ExecutionWrapperConfig> sections) {
    this.sections = sections;
  }
}
