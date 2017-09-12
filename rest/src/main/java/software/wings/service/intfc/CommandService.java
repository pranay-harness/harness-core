package software.wings.service.intfc;

import software.wings.beans.command.Command;
import software.wings.beans.command.ServiceCommand;

import java.util.List;

/**
 * Created by peeyushaggarwal on 11/16/16.
 */
public interface CommandService {
  Command getCommand(String appId, String originEntityId, int version);
  List<Command> getCommandList(String appId, String originEntityId);
  ServiceCommand getServiceCommand(String appId, String serviceCommandId);
  Command save(Command command);
  Command update(Command command);
}
