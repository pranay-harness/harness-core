package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerResizeCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public abstract class ContainerCommandUnitYamlHandler<Y extends ContainerResizeCommandUnit.Yaml, C
                                                          extends ContainerResizeCommandUnit, B
                                                          extends ContainerResizeCommandUnit.Yaml.Builder>
    extends CommandUnitYamlHandler<Y, C, B> {}
