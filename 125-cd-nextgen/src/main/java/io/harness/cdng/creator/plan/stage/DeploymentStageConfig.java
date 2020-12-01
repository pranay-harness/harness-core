package io.harness.cdng.creator.plan.stage;

import io.harness.beans.ParameterField;
import io.harness.cdng.creator.plan.execution.ExecutionElementConfig;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.common.SwaggerConstants;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonTypeName("Deployment")
@TypeAlias("deploymentStageConfig")
public class DeploymentStageConfig {
  String uuid;
  List<NGVariable> variables;
  ServiceConfig service;
  PipelineInfrastructure infrastructure;
  ExecutionElementConfig execution;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> skipCondition;
}
