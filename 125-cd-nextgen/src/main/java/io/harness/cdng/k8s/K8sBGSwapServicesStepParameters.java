package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
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

@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sBGSwapServicesStepParameters")
public class K8sBGSwapServicesStepParameters implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sBGSwapServicesStepParameters.infoBuilder()
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .description(description)
                                                       .skipCondition(skipCondition)
                                                       .skipDryRun(skipDryRun)
                                                       .build());
  }

  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBGSwapServicesStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      RollbackInfo rollbackInfo) {
    this.name = name;
    this.identifier = identifier;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.description = description;
    this.skipCondition = skipCondition;
    this.skipDryRun = skipDryRun;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Collections.singletonList(K8sCommandUnitConstants.SwapServiceSelectors);
  }
}
