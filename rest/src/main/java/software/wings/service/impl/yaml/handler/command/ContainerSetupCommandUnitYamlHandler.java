package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerSetupCommandUnit;

/**
 * @author brett on 11/28/17
 */
public abstract class ContainerSetupCommandUnitYamlHandler<Y extends ContainerSetupCommandUnit.Yaml, C
                                                               extends ContainerSetupCommandUnit, B
                                                               extends ContainerSetupCommandUnit.Yaml.Builder>
    extends CommandUnitYamlHandler<Y, C, B> {}
