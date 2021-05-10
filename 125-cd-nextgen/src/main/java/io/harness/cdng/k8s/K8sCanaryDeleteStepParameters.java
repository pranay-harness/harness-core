package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.yaml.ParameterField;

import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sCanaryDeleteParameters")
public class K8sCanaryDeleteStepParameters implements K8sSpecParameters {
  ParameterField<Boolean> skipDryRun;

  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryDeleteStepParameters(ParameterField<Boolean> skipDryRun) {
    this.skipDryRun = skipDryRun;
  }

  @Override
  public List<String> getCommandUnits() {
    return Arrays.asList(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);
  }
}
