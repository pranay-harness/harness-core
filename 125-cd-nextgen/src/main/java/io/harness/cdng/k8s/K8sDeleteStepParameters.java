package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("k8sDeleteStepParameters")
public class K8sDeleteStepParameters extends K8sDeleteBaseStepInfo implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sDeleteStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, DeleteResourcesWrapper deleteResources, ParameterField<String> timeout,
      ParameterField<Boolean> skipDryRun, RollbackInfo rollbackInfo) {
    super(deleteResources, skipDryRun);
    this.name = name;
    this.identifier = identifier;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sDeleteStepParameters.infoBuilder()
                                                       .deleteResources(this.getDeleteResources())
                                                       .skipDryRun(skipDryRun)
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .description(description)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .build());
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    if (deleteResources != null && deleteResources.getType() == DeleteResourcesType.ManifestPath) {
      return Arrays.asList(
          K8sCommandUnitConstants.FetchFiles, K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);
    }
    return Arrays.asList(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);
  }
}
