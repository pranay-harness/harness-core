package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SshCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
public abstract class SshCommandUnitYamlHandler<Y extends SshCommandUnit.Yaml, C extends SshCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
