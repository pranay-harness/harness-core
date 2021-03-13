package software.wings.verification.apm;

import static software.wings.verification.CVConfiguration.CVConfigurationYaml;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.APMVerificationState;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class APMCVConfigurationYaml extends CVConfigurationYaml {
  private List<APMVerificationState.MetricCollectionInfo> metricCollectionInfos;
}
