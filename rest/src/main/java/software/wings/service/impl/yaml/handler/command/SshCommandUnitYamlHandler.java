package software.wings.service.impl.yaml.handler.command;

import software.wings.beans.command.SshCommandUnit;
import software.wings.beans.yaml.ChangeContext;

import java.util.Map;

/**
 * @author rktummala on 11/13/17
 */
public abstract class SshCommandUnitYamlHandler<Y extends SshCommandUnit.Yaml, C extends SshCommandUnit>
    extends CommandUnitYamlHandler<Y, C> {
  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<Y> changeContext) {
    return super.getNodeProperties(changeContext);
  }
}
