package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SshCommandUnit;
import software.wings.beans.command.SshCommandUnitYaml;

/**
 * @author rktummala on 11/13/17
 */
public abstract class SshCommandUnitYamlHandler<Y extends SshCommandUnitYaml, C extends SshCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
