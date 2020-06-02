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
@JsonTypeName(DeploymentStage.DEPLOYMENT)
public class DeploymentStage implements CDStage {
  public static final String DEPLOYMENT = "deployment";
  private final String type = DEPLOYMENT;
  private String name;
  private boolean runParallel;
  private String skipCondition;
  private String description;
  private String identifier;
  private PipelineInfrastructure infrastructure;
  private Service service;
  private StageVariables stageVariables;
  List<PhaseWrapper> execution;
}
