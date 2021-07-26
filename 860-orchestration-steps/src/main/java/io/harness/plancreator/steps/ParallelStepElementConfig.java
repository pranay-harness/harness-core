package io.harness.plancreator.steps;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.plancreator.execution.ExecutionWrapperConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
// TODO this should go to yaml commons
@TargetModule(HarnessModule._884_PMS_COMMONS)
@OwnedBy(PIPELINE)
@TypeAlias("io.harness.yaml.core.parallelStepElementConfig")
public class ParallelStepElementConfig {
  @NotNull List<ExecutionWrapperConfig> sections;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ParallelStepElementConfig(List<ExecutionWrapperConfig> sections) {
    this.sections = sections;
  }
}
