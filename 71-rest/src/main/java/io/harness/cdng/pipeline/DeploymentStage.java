package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.service.beans.ServiceConfig;
import io.harness.cdng.variables.StageVariables;
import io.harness.yaml.core.ExecutionElement;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@JsonTypeName(DeploymentStage.DEPLOYMENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeploymentStage implements CDStage {
  @JsonIgnore public static final String DEPLOYMENT_NAME = "Deployment";
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore String name;
  ServiceConfig service;
  PipelineInfrastructure infrastructure;
  ExecutionElement execution;
  StageVariables stageVariables;
  String skipCondition;
}
