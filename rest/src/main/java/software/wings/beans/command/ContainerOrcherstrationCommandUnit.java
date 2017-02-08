package software.wings.beans.command;

import com.google.inject.Inject;

import software.wings.cloudprovider.ClusterService;

/**
 * Created by peeyushaggarwal on 2/1/17.
 */
public abstract class ContainerOrcherstrationCommandUnit extends AbstractCommandUnit {
  @Inject protected ClusterService clusterService;

  public ContainerOrcherstrationCommandUnit() {
    super();
  }

  /**
   * Instantiates a new command unit.
   *
   * @param commandUnitType the command unit type
   */
  public ContainerOrcherstrationCommandUnit(CommandUnitType commandUnitType) {
    super(commandUnitType);
  }
}
