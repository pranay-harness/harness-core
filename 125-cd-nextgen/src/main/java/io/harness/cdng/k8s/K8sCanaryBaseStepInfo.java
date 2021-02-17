package io.harness.cdng.k8s;

import io.harness.pms.yaml.ParameterField;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sCanaryBaseStepInfo")
public class K8sCanaryBaseStepInfo {
  @NotNull InstanceSelectionWrapper instanceSelection;
  ParameterField<Boolean> skipDryRun;
}
