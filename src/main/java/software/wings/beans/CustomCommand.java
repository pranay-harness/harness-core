package software.wings.beans;

import static software.wings.beans.ExecCommandUnit.ExecCommandUnitBuilder.anExecCommandUnit;

import java.util.Arrays;
import java.util.List;

/**
 * Created by anubhaw on 5/24/16.
 */
public class CustomCommand extends Execution {
  @Override
  public List<CommandUnit> getCommandUnits() {
    ExecCommandUnit setup =
        anExecCommandUnit().withCommandString("rm -rf wings && mkdir -p $HOME/wings/downloads").build();
    ExecCommandUnit run = anExecCommandUnit().withCommandString("sh /bin/start.sh").build();
    return Arrays.asList(setup, run);
  }
}
