package io.harness.beans.stages;

import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.data.validator.EntityIdentifier;
import io.harness.pipeline.executions.NGStageType;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.intfc.Connector;
import io.harness.yaml.extended.ci.container.Container;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.springframework.data.annotation.TypeAlias;

/**
 *  This Stage stores steps required for running CI job.
 *  It will execute all steps serially.
 *  Stores identifier for kubernetes cluster.
 */

@Data
@Builder
@JsonTypeName("CI")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("integrationStage")
public class IntegrationStage implements CIStage {
  @JsonIgnore public static final CIStageType type = CIStageType.ci;
  @JsonIgnore
  public static final NGStageType INTEGRATION_STAGE_TYPE = NGStageType.builder().type(CIStageType.ci.name()).build();

  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @NotNull @EntityIdentifier private String identifier;
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore private String name;

  private boolean runParallel;
  private boolean skipGitClone;

  private List<String> sharedPaths;

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
