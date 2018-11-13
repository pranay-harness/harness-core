package software.wings.beans.command;

import static software.wings.beans.command.CommandUnitType.K8S_DUMMY;

import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;

public class K8sDummyCommandUnit extends AbstractCommandUnit {
  public static final String Init = "Initialize";
  public static final String Prepare = "Prepare";
  public static final String Apply = "Apply";
  public static final String Rollback = "Rollback";
  public static final String StatusCheck = "Status Check";

  public K8sDummyCommandUnit(String name) {
    super(K8S_DUMMY);
    setName(name);
  }

  public K8sDummyCommandUnit() {
    super(K8S_DUMMY);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
