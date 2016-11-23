package software.wings.service.intfc;

import software.wings.beans.command.Command;

/**
 * Created by peeyushaggarwal on 11/16/16.
 */
public interface CommandService {
  Command getCommand(String appId, String originEntityId, int version);
  Command save(Command command);
  Command update(Command command);
}
