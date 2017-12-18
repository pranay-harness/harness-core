package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerResizeCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public abstract class ContainerResizeCommandUnitYamlHandler<Y extends ContainerResizeCommandUnit.Yaml, C
                                                                extends ContainerResizeCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
