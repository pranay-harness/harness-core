package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.ContainerSetupCommandUnit;
import software.wings.beans.command.ContainerSetupCommandUnitYaml;

/**
 * @author brett on 11/28/17
 */
public abstract class ContainerSetupCommandUnitYamlHandler<Y extends ContainerSetupCommandUnitYaml, C
                                                               extends ContainerSetupCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
