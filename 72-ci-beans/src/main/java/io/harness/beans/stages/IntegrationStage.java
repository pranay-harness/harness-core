package io.harness.beans.stages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pipeline.executions.NGStageType;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.core.intfc.Infrastructure;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

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
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntegrationStage implements CIStage {
  @JsonIgnore public static final CIStageType type = CIStageType.INTEGRATION;
  @JsonIgnore
  public static final NGStageType INTEGRATION_STAGE_TYPE =
      NGStageType.builder().type(CIStageType.INTEGRATION.name()).build();

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @NotNull @EntityIdentifier private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String name;

  private boolean runParallel;
  private String skipCondition;

  private Infrastructure infrastructure;
  private Connector gitConnector;
  private Container container;
  private String workingDirectory;

  private List<CustomVariable> customVariables;

  @NotNull private ExecutionElement execution;
  private List<DependencyElement> dependencies;

  @Override
  public CIStageType getType() {
    return type;
  }

  @Override
  public NGStageType getStageType() {
    return INTEGRATION_STAGE_TYPE;
  }
}
