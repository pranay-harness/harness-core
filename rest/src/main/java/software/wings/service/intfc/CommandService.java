package software.wings.service.intfc;

import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;

/**
 * Created by peeyushaggarwal on 11/16/16.
 */
public interface CommandService {
  Command getCommand(String appId, String originEntityId, int version);
  ServiceCommand getServiceCommand(String appId, String serviceCommandId);
  ServiceCommand getServiceCommandByName(String appId, String serviceId, String serviceCommandName);
  Command save(Command command, boolean pushToYaml);
  Command update(Command command, boolean pushToYaml);
}
