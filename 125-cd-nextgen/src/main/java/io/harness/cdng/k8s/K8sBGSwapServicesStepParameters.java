package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sBGSwapServicesStepParameters")
@RecasterAlias("io.harness.cdng.k8s.K8sBGSwapServicesStepParameters")
public class K8sBGSwapServicesStepParameters extends K8sBGSwapServicesStepInfo implements K8sSpecParameters {
  @Builder(builderMethodName = "infoBuilder")
  public K8sBGSwapServicesStepParameters(ParameterField<Boolean> skipDryRun,
      ParameterField<List<TaskSelectorYaml>> delegateSelectors, String blueGreenStepFqn,
      String blueGreenSwapServicesFqn) {
    this.skipDryRun = skipDryRun;
    this.delegateSelectors = delegateSelectors;
    this.blueGreenStepFqn = blueGreenStepFqn;
    this.blueGreenSwapServicesStepFqn = blueGreenSwapServicesFqn;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Collections.singletonList(K8sCommandUnitConstants.SwapServiceSelectors);
  }
}
