package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.variables.StageVariables;
import io.harness.yaml.core.auxiliary.intfc.PhaseWrapper;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonTypeName(DeploymentStage.DEPLOYMENT_NAME)
public class DeploymentStage implements CDStage {
  public static final String DEPLOYMENT_NAME = "deployment";
  String identifier;
  String displayName;
  Deployment deployment;

  @Value
  @Builder
  public static class Deployment {
    boolean runParallel;
    String skipCondition;
    String description;
    PipelineInfrastructure infrastructure;
    ServiceConfig service;
    StageVariables stageVariables;
    List<PhaseWrapper> execution;
  }
}
