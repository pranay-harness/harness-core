package software.wings.beans.command;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import software.wings.stencils.DefaultValue;

/**
 * Created by anubhaw on 7/12/16.
 */
public class SetupEnvCommandUnit extends ExecCommandUnit {
  /**
   * Instantiates a new Setup env command unit.
   */
  public SetupEnvCommandUnit() {
    super();
    setCommandUnitType(CommandUnitType.SETUP_ENV);
    setCommandString("mkdir -p $WINGS_RUNTIME_PATH\n"
        + "mkdir -p $WINGS_BACKUP_PATH\n"
        + "mkdir -p $WINGS_STAGING_PATH");
  }

  @SchemaIgnore
  @Override
  public String getPreparedCommand() {
    return super.getPreparedCommand();
  }

  @Attributes(title = "Command")
  @DefaultValue("mkdir -p $WINGS_RUNTIME_PATH\n"
      + "mkdir -p $WINGS_BACKUP_PATH\n"
      + "mkdir -p $WINGS_STAGING_PATH")
  @Override
  public String
  getCommandString() {
    return super.getCommandString();
  }

  @Attributes(title = "Execution Directory", description = "Relative to ${RuntimePath}")
  @Override
  public String getCommandPath() {
    return super.getCommandPath();
  }
}
