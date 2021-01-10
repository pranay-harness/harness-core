package io.harness.cdng.creator.plan.stage;

import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.common.SwaggerConstants;
import io.harness.plancreator.execution.ExecutionElementConfig;
import io.harness.plancreator.stages.stage.StageInfoConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("Deployment")
@TypeAlias("deploymentStageConfig")
public class DeploymentStageConfig implements StageInfoConfig {
  String uuid;
  List<NGVariable> variables;
  ServiceConfig serviceConfig;
  PipelineInfrastructure infrastructure;
  ExecutionElementConfig execution;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
}
