package io.harness.beans.stages;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.yaml.core.Artifact;
import io.harness.yaml.core.Execution;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import io.harness.yaml.core.intfc.Stage;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *  This Stage stores steps required for running CI job.
 *  It will execute all steps serially.
 *  Stores identifier for kubernetes cluster.
 */

@Data
@Builder
@JsonTypeName("integration")
public class IntegrationStage implements Stage {
  // Default properties
  @NotNull private String identifier;
  private String name;
  @NotNull private String type;
  private boolean runParallel;
  private String skipCondition;
  private String description;

  // CI specific properties
  private String location;
  private String project;
  private String image;

  private Infrastructure infrastructure;
  private Connector connector;
  private Artifact artifact;
  private Container container;

  private String workingDirectory;

  @NotNull private Execution execution;
}
