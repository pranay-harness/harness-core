package software.wings.beans.command;

import static software.wings.beans.command.CommandUnitType.PCF_DUMMY;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import org.apache.commons.lang3.NotImplementedException;

public class PcfDummyCommandUnit extends AbstractCommandUnit {
  public static final String FetchFiles = "Download Manifest Files";
  public static final String CheckExistingApps = "Check Existing Applications";
  public static final String PcfSetup = "Setup Application";
  public static final String Wrapup = "Wrap up";
  public static final String Pcfplugin = "Execute CF Command";
  public static final String Downsize = "Downsize Application";
  public static final String Upsize = "Upsize Application";

  public PcfDummyCommandUnit(String name) {
    super(PCF_DUMMY);
    setName(name);
  }

  @Override
  public CommandExecutionStatus execute(CommandExecutionContext context) {
    throw new NotImplementedException("Not implemented");
  }
}
