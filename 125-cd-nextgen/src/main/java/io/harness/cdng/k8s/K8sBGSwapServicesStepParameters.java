package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
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
public class K8sBGSwapServicesStepParameters implements K8sSpecParameters {
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBGSwapServicesStepParameters(ParameterField<Boolean> skipDryRun) {
    this.skipDryRun = skipDryRun;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Collections.singletonList(K8sCommandUnitConstants.SwapServiceSelectors);
  }
}
