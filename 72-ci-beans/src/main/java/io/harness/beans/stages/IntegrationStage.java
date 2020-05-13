package io.harness.beans.stages;

import com.fasterxml.jackson.annotation.JsonTypeName;
import graph.Graph;
import io.harness.beans.steps.CIStep;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

/**
 *  This Stage stores steps required for running CI job.
 *  It will execute all steps serially.
 *  Stores identifier for kubernetes cluster.
 */

@JsonTypeName("INTEGRATION")
@Data
@Value
@Builder
public class IntegrationStage implements StageInfo {
  @NotNull private StageType type = StageType.INTEGRATION;
  Graph<CIStep> stepInfos;
  private String description;
  @NotEmpty private String k8ConnectorIdentifier;
  @NotEmpty private String identifier;
  @Override
  public StageType getType() {
    return type;
  }
}
