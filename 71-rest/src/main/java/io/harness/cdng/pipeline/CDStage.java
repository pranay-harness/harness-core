package io.harness.cdng.pipeline;

import io.harness.cdng.service.Service;
import io.harness.yaml.core.intfc.Stage;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Builder
public class CDStage implements Stage {
  private String name;
  private boolean runParallel;
  private String skipCondition;
  private String description;
  private String identifier;
  private PipelineInfrastructure infrastructure;
  private Service service;

  @NotNull
  @Override
  public String getType() {
    return "CD Stage";
  }
}
