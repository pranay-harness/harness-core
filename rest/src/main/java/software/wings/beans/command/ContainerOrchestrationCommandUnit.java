package software.wings.beans.command;

import com.google.inject.Inject;

import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.cloudprovider.ClusterService;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class ContainerOrchestrationCommandUnit extends AbstractCommandUnit {
  @Inject @Transient protected ClusterService clusterService;

  public ContainerOrchestrationCommandUnit() {
    super();
    setArtifactNeeded(true);
  }

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public ContainerOrchestrationCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }

  @Override
  public String deploymentType() {
    return DeploymentType.ECS.name(); // TODO: fix it for other tyes. eg. Kubernetes
  }
}
