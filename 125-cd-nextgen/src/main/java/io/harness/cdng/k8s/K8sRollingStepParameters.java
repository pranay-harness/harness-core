package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("k8sRollingStepParameters")
public class K8sRollingStepParameters extends K8sRollingBaseStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingStepParameters(ParameterField<Boolean> skipDryRun) {
    super(skipDryRun);
  }
}
