package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerResizeCommandUnit;
import software.wings.beans.command.ContainerResizeCommandUnitYaml;

/**
 * @author rktummala on 11/13/17
 */
public abstract class ContainerResizeCommandUnitYamlHandler<Y extends ContainerResizeCommandUnitYaml, C
                                                                extends ContainerResizeCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
