package io.harness.cdng.pipeline;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.service.Service;
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
  String type = DEPLOYMENT_NAME;
  String identifier;
  String name;
  Deployment deployment;

  @Value
  @Builder
  public static class Deployment {
    boolean runParallel;
    String skipCondition;
    String description;
    PipelineInfrastructure infrastructure;
    Service service;
    StageVariables stageVariables;
    List<PhaseWrapper> execution;
  }
}
