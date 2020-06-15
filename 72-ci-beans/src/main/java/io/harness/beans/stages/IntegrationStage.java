package io.harness.beans.stages;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.yaml.extended.CustomVariables;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.data.validator.EntityIdentifier;
import io.harness.yaml.core.Artifact;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

/**
 *  This Stage stores steps required for running CI job.
 *  It will execute all steps serially.
 *  Stores identifier for kubernetes cluster.
 */

@Data
@Builder
@JsonTypeName("ci")
public class IntegrationStage implements CIStage {
  private static final CIStageType type = CIStageType.INTEGRATION;

  @NotNull @EntityIdentifier private String identifier;
  private String name;
  private Integration ci;

  @Override
  public CIStageType getType() {
    return type;
  }

  @Data
  @Builder
  public static class Integration {
    private boolean runParallel;
    private String skipCondition;
    private String description;

    private Infrastructure infrastructure;
    private Connector connector;
    private Artifact artifact;
    private Container container;

    private List<CustomVariables> customVariables;

    private String workingDirectory;

    @NotNull private Execution execution;
  }
}
