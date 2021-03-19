package software.wings.service.impl.yaml.handler.command;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.command.SshCommandUnit;

/**
 * @author rktummala on 11/13/17
 */
@OwnedBy(CDP)
public abstract class SshCommandUnitYamlHandler<Y extends SshCommandUnit.Yaml, C extends SshCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {}
